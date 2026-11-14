import { describe, expect, it, vi } from 'vitest';
import type { SupabaseClient } from '@supabase/supabase-js';
import { signInWithProvider } from '../auth';

const clientWith = (signInWithOAuth: ReturnType<typeof vi.fn>) =>
  ({ auth: { signInWithOAuth } } as unknown as SupabaseClient);

describe('signInWithProvider', () => {
  it('starts the OAuth flow and forwards the redirect target', async () => {
    const signInWithOAuth = vi
      .fn()
      .mockResolvedValue({ data: { provider: 'google', url: 'https://accounts.google.com' }, error: null });

    const result = await signInWithProvider(clientWith(signInWithOAuth), 'google', 'https://gambit.example/');

    expect(signInWithOAuth).toHaveBeenCalledWith({
      provider: 'google',
      options: { redirectTo: 'https://gambit.example/' }
    });
    expect(result.error).toBeNull();
  });

  it('surfaces the provider error message', async () => {
    const signInWithOAuth = vi.fn().mockResolvedValue({
      data: { provider: 'github', url: null },
      error: { message: 'Unsupported provider: provider is not enabled' }
    });

    const result = await signInWithProvider(clientWith(signInWithOAuth), 'github', 'https://gambit.example/');

    expect(result.error).toBe('Unsupported provider: provider is not enabled');
  });
});
