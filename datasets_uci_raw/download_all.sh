#!/bin/bash
# Download 26 datasets từ UCI (theo link mới /dataset/ID/)

BASE="https://archive.ics.uci.edu/static/public"
dl() {
  local id=$1 name=$2
  curl -sSL -o "${name}.zip" "${BASE}/${id}/${name// /+}.zip" \
    && echo "OK ${name}.zip ($(wc -c < ${name}.zip) bytes)" \
    || echo "FAIL ${name}.zip"
}

# id | folder name
dl 3   "annealing" &
dl 143 "statlog+australian+credit+approval" &
dl 10  "automobile" &
dl 15  "breast+cancer+wisconsin+original" &
dl 45  "heart+disease" &
dl 27  "credit+approval" &
dl 144 "statlog+german+credit+data" &
dl 42  "glass+identification" &
dl 145 "statlog+heart" &
dl 46  "hepatitis" &
dl 47  "horse+colic" &
dl 102 "thyroid+disease" &
dl 52  "ionosphere" &
dl 53  "iris" &
dl 57  "labor+relations" &
dl 59  "led+display+domain" &
dl 63  "lymphography" &
dl 151 "connectionist+bench+sonar+mines+vs+rocks" &
dl 101 "tic+tac+toe+endgame" &
dl 149 "statlog+vehicle+silhouettes" &
dl 107 "waveform+database+generator+version+1" &
dl 109 "wine" &
dl 111 "zoo" &
wait
echo "=== Download done ==="
ls -la *.zip 2>/dev/null | wc -l
