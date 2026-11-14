import { afterEach, describe, expect, it, vi } from 'vitest';

async function loadModule() {
  vi.resetModules();
  return import('../supabase');
}

afterEach(() => {
  vi.unstubAllEnvs();
  vi.unstubAllGlobals();
});

describe('supabase client', () => {
  it('is null when env vars are missing', async () => {
    vi.stubEnv('VITE_SUPABASE_URL', '');
    vi.stubEnv('VITE_SUPABASE_ANON_KEY', '');
    const { supabase, isSupabaseEnabled } = await loadModule();
    expect(supabase).toBeNull();
    expect(isSupabaseEnabled).toBe(false);
  });

  it('is null when only one env var is set', async () => {
    vi.stubEnv('VITE_SUPABASE_URL', 'https://example.supabase.co');
    vi.stubEnv('VITE_SUPABASE_ANON_KEY', '');
    const { supabase } = await loadModule();
    expect(supabase).toBeNull();
  });

  it('is null instead of throwing for an invalid URL', async () => {
    vi.stubEnv('VITE_SUPABASE_URL', 'not a url');
    vi.stubEnv('VITE_SUPABASE_ANON_KEY', 'anon-key');
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
    const { supabase } = await loadModule();
    expect(supabase).toBeNull();
    expect(warn).toHaveBeenCalled();
    warn.mockRestore();
  });

  it('creates a client when both env vars are set', async () => {
    // Browsers have WebSocket; Node 20 (the test runtime) does not, and realtime-js needs one.
    if (typeof globalThis.WebSocket === 'undefined') vi.stubGlobal('WebSocket', class {});
    vi.stubEnv('VITE_SUPABASE_URL', 'https://example.supabase.co');
    vi.stubEnv('VITE_SUPABASE_ANON_KEY', 'anon-key');
    const { supabase, isSupabaseEnabled } = await loadModule();
    expect(supabase).not.toBeNull();
    expect(typeof supabase!.from).toBe('function');
    expect(isSupabaseEnabled).toBe(true);
  });
});
