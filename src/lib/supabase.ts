import { createClient, SupabaseClient } from '@supabase/supabase-js';

const url = import.meta.env.VITE_SUPABASE_URL;
const anonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;

function initSupabase(): SupabaseClient | null {
  if (!url || !anonKey) {
    if (import.meta.env.DEV) {
      console.info('[supabase] VITE_SUPABASE_URL / VITE_SUPABASE_ANON_KEY not set; online features disabled.');
    }
    return null;
  }
  try {
    return createClient(url, anonKey);
  } catch (err) {
    console.warn('[supabase] Failed to initialize client; online features disabled.', err);
    return null;
  }
}

/** `null` when Supabase is not configured; callers must fall back to local storage. */
export const supabase: SupabaseClient | null = initSupabase();

export const isSupabaseEnabled = supabase !== null;
