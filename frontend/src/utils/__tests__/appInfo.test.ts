import { describe, expect, it } from 'vitest';
import { shortVersion, versionsDiffer } from '../appInfo';

describe('shortVersion', () => {
  it('shortens commit SHAs to 7 characters', () => {
    expect(shortVersion('55ae374f0c1d2e3b4a5968778695a4b3c2d1e0f9')).toBe('55ae374');
  });

  it('keeps dev, tags and missing values readable', () => {
    expect(shortVersion('dev')).toBe('dev');
    expect(shortVersion('')).toBe('dev');
    expect(shortVersion(null)).toBe('dev');
    expect(shortVersion('v0.2.0')).toBe('v0.2.0');
  });
});

describe('versionsDiffer', () => {
  const sha = '55ae374f0c1d2e3b4a5968778695a4b3c2d1e0f9';

  it('flags CI builds from different commits', () => {
    expect(versionsDiffer(sha, 'c885f48a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e')).toBe(true);
  });

  it('does not flag equal, local or unknown versions', () => {
    expect(versionsDiffer(sha, sha)).toBe(false);
    expect(versionsDiffer('dev', sha)).toBe(false);
    expect(versionsDiffer(sha, 'dev')).toBe(false);
    expect(versionsDiffer(sha, null)).toBe(false);
  });
});
