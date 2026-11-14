# AGENTS.md

Repository notes for future agent sessions.

## Commands

- `npm ci` / `npm install` — install deps
- `npm run build` — `tsc && vite build` (TypeScript is strict; this catches most issues)
- `npm test` — `vitest run`
- `npm run dev` — Vite dev server on port 3000

## Testing

- Tests live in `__tests__/` next to the code (`src/domain/__tests__`, `src/services/__tests__`, `src/lib/__tests__`).
- There is **no DOM test environment** (no jsdom / testing-library). Tests exercise pure
  functions and modules with a mock client, e.g. `src/services/__tests__/cloudSync.test.ts`
  passes a `{ rpc } as unknown as SupabaseClient`. Follow that pattern for services;
  do not try to render components in tests without first adding a DOM environment.

## Supabase / auth

- `src/lib/supabase.ts` exports `supabase: SupabaseClient | null`. It is `null` when
  `VITE_SUPABASE_URL` / `VITE_SUPABASE_ANON_KEY` are unset, so the app runs fully offline.
  Always null-check before calling.
- Session state comes from `src/hooks/useSupabaseSession.ts`.
- The Profile screen renders `src/components/AccountPanel.tsx`, which handles sign-in,
  the one-time offline->cloud stats import, and sign-out.
- `supabase/schema.sql` holds the tables and RPCs (`sync_local_stats`, `record_game_result`).
- OAuth needs the provider enabled in the Supabase dashboard; it cannot be verified locally.

## lucide-react icons

This version has **no brand icons** — `Github`, `Google`, `Chrome`, etc. do not exist and
will break the build. Use generic icons (`Globe`, `Code2`) or verify a name exists before
importing it.
