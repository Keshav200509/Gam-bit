/**
 * POST /api/submit-score
 *
 * Serverless (Vercel / Netlify) score-verification endpoint. Validates a
 * submitted game result against the engine's scoring bounds, then writes it to
 * `player_stats` with the service-role key. The browser only ever holds the
 * anon key, so a score can never be written directly from the client.
 *
 * Body:   { userId: string, score: number, turns: number, gameMode: string }
 * 200:    { status: "success", verified: true, stats: {...} }
 * 400:    { status: "rejected", verified: false, reason: string }
 * 405/500:{ status: "error", verified: false, reason: string }
 */
import { createClient } from '@supabase/supabase-js';
import { verifyScoreSubmission } from '../src/services/scoreVerification';

interface VercelRequest {
  method?: string;
  body?: unknown;
}
interface VercelResponse {
  status(code: number): VercelResponse;
  json(payload: unknown): void;
  setHeader(name: string, value: string): void;
  end(): void;
}

/** Server-side only: never expose a VITE_-prefixed service-role key to the bundle. */
function serverEnv(): { url: string; serviceRoleKey: string } | null {
  const url = process.env.SUPABASE_URL ?? process.env.VITE_SUPABASE_URL;
  const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
  if (!url || !serviceRoleKey) return null;
  return { url, serviceRoleKey };
}

/** Vercel parses JSON bodies, but Netlify and raw requests may hand us a string. */
function parseBody(body: unknown): unknown {
  if (typeof body === 'string') {
    try {
      return JSON.parse(body);
    } catch {
      return undefined;
    }
  }
  return body;
}

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ status: 'error', verified: false, reason: 'Method not allowed' });
  }

  const payload = parseBody(req.body);
  if (payload === undefined) {
    return res.status(400).json({ status: 'rejected', verified: false, reason: 'Invalid JSON body' });
  }

  const verification = verifyScoreSubmission(payload);
  if (!verification.ok) {
    return res.status(400).json({ status: 'rejected', verified: false, reason: verification.reason });
  }

  const env = serverEnv();
  if (!env) {
    return res.status(500).json({
      status: 'error',
      verified: false,
      reason: 'Score verification is not configured (missing Supabase service role credentials)'
    });
  }

  const { userId, score, turns, gameMode } = verification.submission;
  const supabase = createClient(env.url, env.serviceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false }
  });

  const { data, error } = await supabase.rpc('submit_verified_score', {
    p_user_id: userId,
    p_score: score,
    p_turns: turns,
    p_game_mode: gameMode
  });

  if (error) {
    return res.status(502).json({ status: 'error', verified: false, reason: error.message });
  }

  return res.status(200).json({ status: 'success', verified: true, stats: data });
}
