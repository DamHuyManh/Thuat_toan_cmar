const fs = require("fs");
const s = fs.readFileSync(process.argv[2], "utf8");

function esc(str) {
  let o = "";
  for (const c of str) {
    const u = c.codePointAt(0);
    if (u > 127) o += "\\u" + u.toString(16).padStart(4, "0");
    else if (c === "\\") o += "\\\\";
    else if (c === '"') o += '\\"';
    else if (c === "\n") o += "\\n";
    else if (c === "\r") continue;
    else o += c;
  }
  return o;
}

for (const line of s.split(/\n/)) {
  console.log('        sb.append("' + esc(line) + '\\n");');
}
