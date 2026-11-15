# Gambit

A 2D turn-based tactical strategy game built in React, TypeScript, Vite, and Tailwind CSS.

## Features

- **5x5 Tactical Grid**: Territory control across 12 strategic rounds with high-value clusters, golden cells, volatile sectors, and battlegrounds.
- **Dynamic AI Opponents**: Dynamic cognitive states (Peak, Sharp, Steady, Sloppy) with distinct strategic modes across three theaters:
  - **Ascendency**: Human-first with reactive AI counters.
  - **Confrontation**: Simultaneous secret commitments.
  - **Oblivion**: Total information blackout and high bluff rates.
- **Signals Intelligence & Recon**: Decodable intelligence intercepts, reliability scoring, and scout drones.
- **Tactical Clashes**: Token deployments (1–5), friendly neighbor adjacencies, defensive fortifications, and tactical Locks (+2 strength).
- **Daily Operations & Leaderboard**: Synchronized daily challenge seeds with global community standings.
- **Social & Local PvP**: Two-player hotseat duel and fleet communications.
- **Career Dossier**: Detailed stats tracking, 12 achievement medals, match logs, and customizable callsign / insignia.

## Development

```bash
npm install
npm run dev
```

Build for production:
```bash
npm run build
```

## Online features (Supabase)

The game runs fully offline by default. To enable accounts, cloud stats and online
leaderboards, copy `.env.example` to `.env` and fill in:

```
VITE_SUPABASE_URL=...
VITE_SUPABASE_ANON_KEY=...
```

Then apply `supabase/schema.sql` to your project. Without these values the Profile
screen shows Guest Mode and everything stays in `localStorage`.

### Sign-in methods

Email/password works out of the box. Google and GitHub sign-in also require the
provider to be enabled in the Supabase dashboard (**Authentication -> Providers**),
and the app origin (for example `http://localhost:3000`) added under
**Authentication -> URL Configuration -> Redirect URLs**. If a provider is not
enabled, the Profile screen shows the error returned by Supabase.

On the first sign-in, the player's offline career is imported once into
`player_stats` via the `sync_local_stats` RPC; later sign-ins are no-ops.


### Leaderboard

The leaderboard has two tabs. **Global** reads live standings from `player_stats`
joined to `profiles`, ordered by `high_score` then `elo`; it shows a loading state
and a retry button if the request fails. **This Device** is the offline view and
needs no backend. Without the env vars above the screen opens on This Device, and
Global shows an "uplink not configured" notice.

### Score verification endpoint

`api/submit-score.ts` is a serverless function that validates a submitted result
before it can reach `player_stats`. The browser only holds the anon key, so a
client cannot write stats directly; this route is the write path.

```
POST /api/submit-score
{ "userId": "<uuid>", "score": 142, "turns": 12, "gameMode": "ASCENDENCY" }

200 { "status": "success", "verified": true, "stats": { ... } }
400 { "status": "rejected", "verified": false, "reason": "..." }
```

It rejects `score <= 0`, `turns <= 0`, and any score above the theoretical
points-per-turn ceiling derived from `src/domain/incomeCalculator.ts` (25 cells x
4 value x 2 golden-cell x 2 round multiplier = 400 per turn). Valid submissions
are written through the `submit_verified_score` RPC, which is granted to
`service_role` only. Set these server-side environment variables (not `VITE_`):

```
SUPABASE_URL=...                  # optional, falls back to VITE_SUPABASE_URL
SUPABASE_SERVICE_ROLE_KEY=...
```

