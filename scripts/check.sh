#!/usr/bin/env bash
# Uploads files on one node and confirms them on the other, then checks that the confirmed file
# has the same SHA-256 as the one sent. Run it after `docker compose up`, once for each STRUTS
# value, to see that the NES trial behaves like Apache Struts 1.1.
#
#   scripts/check.sh            # nodes on localhost:8081 and localhost:8082
#   NODE1=http://host:8081 NODE2=http://host:8082 scripts/check.sh
#
# Needs bash and curl. Exits non-zero if any check fails.
set -uo pipefail

NODE1=${NODE1:-http://localhost:8081}
NODE2=${NODE2:-http://localhost:8082}
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
FAILED=0

sha256() {
  if command -v sha256sum >/dev/null; then sha256sum "$1" | cut -d' ' -f1; else shasum -a 256 "$1" | cut -d' ' -f1; fi
}
field() {  # field <page> <pattern>: the first capture of a sed pattern, or "-"
  local v
  v=$(sed -n "s|.*$2.*|\1|p" "$1" | head -1)
  echo "${v:--}"
}
report() {  # report <PASS|FAIL> <name> <detail>
  [ "$1" = PASS ] || FAILED=1
  printf '%-4s  %-34s %s\n' "$1" "$2" "$3"
}

# journey <name> <context and mapping prefix> <upload node> <confirm node> <bytes> <confirm file part>
#   confirm file part: "empty" (an empty file input, as a browser sends it), "none" (no file part),
#   or a number of bytes for a replacement file.
journey() {
  local name=$1 prefix=$2 up=$3 down=$4 bytes=$5 part=$6
  local jar=$WORK/cookies file=$WORK/upload.bin want status review done
  rm -f "$jar"
  head -c "$bytes" /dev/urandom > "$file"
  want=$(sha256 "$file")
  curl -s -c "$jar" -o /dev/null "$up/${prefix%%/*}/start.do"
  status=$(curl -s -b "$jar" -c "$jar" -o "$WORK/review.html" -w '%{http_code}' \
    -F title=Example -F "attachment=@$file;filename=example.bin" "$up/$prefix/upload.do")
  review="upload on $(field "$WORK/review.html" 'id="node">\([^<]*\)<') (HTTP $status)"

  local args=(-F note=checked -F "uploadedOn=$(field "$WORK/review.html" 'id="node">\([^<]*\)<')")
  case $part in
    # A real empty file: with /dev/null, curl sends the request chunked, without the
    # Content-Length that browsers send and Commons FileUpload 1.0 requires.
    empty) : > "$WORK/empty"
           args+=(-F "attachment=@$WORK/empty;filename=") ;;
    none) ;;
    *) head -c "$part" /dev/urandom > "$WORK/replacement.bin"
       want=$(sha256 "$WORK/replacement.bin")
       args+=(-F "attachment=@$WORK/replacement.bin;filename=replacement.bin") ;;
  esac
  status=$(curl -s -b "$jar" -c "$jar" -o "$WORK/done.html" -w '%{http_code}' "${args[@]}" "$down/$prefix/confirm.do")
  done="confirm on $(field "$WORK/done.html" 'id="node">\([^<]*\)<') (HTTP $status)"

  local got held
  got=$(field "$WORK/done.html" 'data-sha256="\([0-9a-f]*\)"')
  held=$(field "$WORK/done.html" 'Held on the form as</th><td><code>\([^<]*\)<' | sed 's/.*\.//')
  if [ "$got" = "$want" ]; then
    report PASS "$name" "$review, $done, held as $held"
  elif grep -q 'id="missing"' "$WORK/done.html"; then
    report FAIL "$name" "$review, $done, no file reached the confirm step"
  else
    report FAIL "$name" "$review, $done, confirmed a $(field "$WORK/done.html" 'data-size="\([0-9]*\)"')-byte file instead"
  fi
}

echo "Struts: $(curl -s "$NODE1/after/start.do" | field /dev/stdin 'Struts jar</th><td>\([^<]*\)<')"
echo

journey "10 KB, empty file input"           after         "$NODE1" "$NODE2" 10000   empty
journey "1 MB, no file part"                after         "$NODE2" "$NODE1" 1000000 none
journey "1 MB, replaced with 10 KB"         after         "$NODE1" "$NODE2" 1000000 10000
journey "DynaActionForm, 10 KB"             after/dyna    "$NODE1" "$NODE2" 10000   empty

# Over maxFileSize="5M", Struts doesn't populate the form and the page says why.
head -c 6000000 /dev/urandom > "$WORK/big.bin"
curl -s -c "$WORK/cookies" -o /dev/null "$NODE1/after/start.do"
curl -s -b "$WORK/cookies" -o "$WORK/big.html" -F title=Example -F "attachment=@$WORK/big.bin" "$NODE1/after/upload.do"
if grep -q 'larger than the 5 MB limit' "$WORK/big.html"; then
  report PASS "6 MB, over maxFileSize" "the upload page says the file is too large"
else
  report FAIL "6 MB, over maxFileSize" "expected the too-large message"
fi

# The same journey without SessionFileRequestProcessor. It should lose the upload.
echo
FAILED_BEFORE=$FAILED
journey "Before: 10 KB, empty file input"   before        "$NODE1" "$NODE2" 10000   empty >"$WORK/before.txt"
FAILED=$FAILED_BEFORE
sed 's/^FAIL/LOST/; s/^PASS/KEPT/' "$WORK/before.txt"
echo "(LOST is the expected result for the before copy.)"

exit $FAILED
