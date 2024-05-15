export function transformUrl(url: string): string {
  if (isGitHub(url)) {
    // Transform https://github.com URLs to https://raw.githubusercontent.com
    // because GitHub applies DDos protection which prevents us from being
    // able to pull the contents of the LICENSE file.
    return rawGithubUrl(url);
  }
  return url;
}

function rawGithubUrl(url: string): string {
  // Transform URL
  const ignoreSet = new Set<string>(['https:', 'github.com', 'blob']);
  const tokens = url.split('/');
  const repo = [];
  const path = [];
  let pathStarted = false;
  for (let i = 0; i < tokens.length; i += 1) {
    if (tokens[i].length <= 0) {
      continue;
    }
    if (tokens[i] === 'blob') {
      pathStarted = true;
    }
    if (ignoreSet.has(tokens[i])) {
      continue;
    }
    if (!pathStarted) {
      repo.push(tokens[i]);
    } else {
      path.push(tokens[i]);
    }
  }
  return `https://raw.githubusercontent.com/${repo.join('/')}/${path.join('/')}`;
}

function isGitHub(url: string): boolean {
  return url.startsWith("https://github.com")
}
