import type { Provider, SupabaseClient } from '@supabase/supabase-js';

/** OAuth providers offered on the Profile screen. */
export type OAuthProvider = Extract<Provider, 'google' | 'github'>;

/**
 * Starts an OAuth sign-in. On success the SDK redirects the browser to the
 * provider, so no session is returned here; `useSupabaseSession` picks the
 * session up from the redirect URL when the player returns.
 *
 * Returns the error message when the flow cannot start, for example a provider
 * that has not been enabled on the Supabase project.
 */
export async function signInWithProvider(
  client: SupabaseClient,
  provider: OAuthProvider,
  redirectTo: string
): Promise<{ error: string | null }> {
  const { error } = await client.auth.signInWithOAuth({ provider, options: { redirectTo } });
  return { error: error ? error.message : null };
}
