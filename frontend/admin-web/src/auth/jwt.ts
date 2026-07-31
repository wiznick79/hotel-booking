export type JwtClaims = {
  sub: string;
  roles: string[];
  permissions: string[];
  hotelIds: string[];
};

export function readJwtClaims(token: string): JwtClaims {
  const payload = token.split('.')[1];

  if (!payload) {
    throw new Error('The access token is malformed.');
  }

  const decodedPayload = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
  const claims = JSON.parse(decodedPayload) as Partial<JwtClaims>;

  return {
    sub: claims.sub ?? '',
    roles: claims.roles ?? [],
    permissions: claims.permissions ?? [],
    hotelIds: claims.hotelIds ?? [],
  };
}
