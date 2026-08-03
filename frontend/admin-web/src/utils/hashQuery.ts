export function readHashQuery() {
  const [, query = ''] = window.location.hash.replace(/^#/, '').split('?');

  return new URLSearchParams(query);
}

export function updateHashQuery(updates: Record<string, string | null>) {
  window.location.hash = createHashWithQuery(updates);
}

export function replaceHashQuery(updates: Record<string, string | null>) {
  const hash = createHashWithQuery(updates);
  const path = `${window.location.pathname}${window.location.search}${hash}`;

  window.history.replaceState(null, '', path);
}

function createHashWithQuery(updates: Record<string, string | null>) {
  const [path = '/'] = window.location.hash.replace(/^#/, '').split('?');
  const parameters = readHashQuery();

  Object.entries(updates).forEach(([key, value]) => {
    if (value === null || value === '') {
      parameters.delete(key);
      return;
    }

    parameters.set(key, value);
  });

  const query = parameters.toString();
  return `#${path || '/'}${query ? `?${query}` : ''}`;
}
