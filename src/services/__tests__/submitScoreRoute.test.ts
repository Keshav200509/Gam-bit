import { afterEach, describe, expect, it, vi } from 'vitest';

const rpc = vi.fn();
vi.mock('@supabase/supabase-js', () => ({
  createClient: vi.fn(() => ({ rpc }))
}));

const { default: handler } = await import('../../../api/submit-score');

function res() {
  const out: { code?: number; body?: unknown; headers: Record<string, string> } = { headers: {} };
  const r = {
    status(code: number) {
      out.code = code;
      return r;
    },
    json(payload: unknown) {
      out.body = payload;
    },
    setHeader(name: string, value: string) {
      out.headers[name] = value;
    },
    end() {}
  };
  return { out, r };
}

const USER = '3f8c1b2e-4a5d-4e6f-8a9b-0c1d2e3f4a5b';
const good = { userId: USER, score: 142, turns: 12, gameMode: 'ASCENDENCY' };

afterEach(() => {
  rpc.mockReset();
  delete process.env.SUPABASE_SERVICE_ROLE_KEY;
  delete process.env.SUPABASE_URL;
});

describe('POST /api/submit-score', () => {
  it('rejects non-POST with 405', async () => {
    const { out, r } = res();
    await handler({ method: 'GET' }, r);
    expect(out.code).toBe(405);
    expect(out.headers.Allow).toBe('POST');
  });

  it('rejects an unparseable string body', async () => {
    const { out, r } = res();
    await handler({ method: 'POST', body: '{not json' }, r);
    expect(out.code).toBe(400);
  });

  it('rejects score <= 0 with 400', async () => {
    const { out, r } = res();
    await handler({ method: 'POST', body: { ...good, score: 0 } }, r);
    expect(out.code).toBe(400);
    expect(out.body).toMatchObject({ status: 'rejected', verified: false });
  });

  it('rejects turns <= 0 with 400', async () => {
    const { out, r } = res();
    await handler({ method: 'POST', body: { ...good, turns: -1 } }, r);
    expect(out.code).toBe(400);
  });

  it('rejects a score above the ceiling with 400', async () => {
    const { out, r } = res();
    await handler({ method: 'POST', body: { ...good, score: 4801 } }, r);
    expect(out.code).toBe(400);
    expect(String((out.body as { reason: string }).reason)).toMatch(/theoretical maximum/);
  });

  it('returns 500 when service credentials are missing', async () => {
    const { out, r } = res();
    await handler({ method: 'POST', body: good }, r);
    expect(out.code).toBe(500);
  });

  it('writes through the RPC and returns verified on success', async () => {
    process.env.SUPABASE_URL = 'https://example.supabase.co';
    process.env.SUPABASE_SERVICE_ROLE_KEY = 'service-role-key';
    rpc.mockResolvedValue({ data: { user_id: USER, high_score: 142 }, error: null });
    const { out, r } = res();
    await handler({ method: 'POST', body: good }, r);
    expect(rpc).toHaveBeenCalledWith('submit_verified_score', {
      p_user_id: USER,
      p_score: 142,
      p_turns: 12,
      p_game_mode: 'ASCENDENCY'
    });
    expect(out.code).toBe(200);
    expect(out.body).toMatchObject({ status: 'success', verified: true });
  });

  it('returns 502 when the RPC fails', async () => {
    process.env.SUPABASE_URL = 'https://example.supabase.co';
    process.env.SUPABASE_SERVICE_ROLE_KEY = 'service-role-key';
    rpc.mockResolvedValue({ data: null, error: { message: 'boom' } });
    const { out, r } = res();
    await handler({ method: 'POST', body: good }, r);
    expect(out.code).toBe(502);
    expect(out.body).toMatchObject({ status: 'error', verified: false, reason: 'boom' });
  });
});
