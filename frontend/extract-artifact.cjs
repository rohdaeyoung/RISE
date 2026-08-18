const fs = require('fs');

const html = fs.readFileSync('dist/index.html', 'utf8');

const viewportMatch = html.match(/<meta name="viewport"[^>]*>/i);
const styleMatches = [...html.matchAll(/<style[\s\S]*?<\/style>/gi)];
const scriptMatches = [...html.matchAll(/<script[\s\S]*?<\/script>/gi)];
const bodyMatch = html.match(/<body[^>]*>([\s\S]*)<\/body>/i);

if (!bodyMatch) throw new Error('no body');
if (scriptMatches.length === 0) throw new Error('no scripts found');

const parts = [
  '<title>WITHU - 오늘 밥 잘 챙겨먹기</title>',
  viewportMatch ? viewportMatch[0] : '',
  ...styleMatches.map((m) => m[0]),
  bodyMatch[1].trim(),
  ...scriptMatches.map((m) => m[0]),
];

const out = parts.filter(Boolean).join('\n');

fs.writeFileSync('dist/artifact.html', out, 'utf8');
console.log('bytes written:', Buffer.byteLength(out, 'utf8'));
console.log('scripts:', scriptMatches.length, 'styles:', styleMatches.length);
