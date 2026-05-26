export const config = {
  bffGraphqlUrl:
    import.meta.env.VITE_BFF_GRAPHQL_URL ??
    `${import.meta.env.VITE_BFF_BASE_URL ?? "http://localhost:9000"}/graphql`,
  region: import.meta.env.VITE_REGION ?? "north-west"
};
