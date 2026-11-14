import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ReadingFormatBadge } from '../ReadingFormatBadge';
import { READING_FORMATS } from '../../types/reading';
import { t } from '../../i18n';

describe('ReadingFormatBadge', () => {
  it('renders nothing when the entry has no format', () => {
    const { container } = render(<ReadingFormatBadge format={null} />);
    expect(container).toBeEmptyDOMElement();
  });

  it.each(READING_FORMATS)('names the %s edition in Polish', (format) => {
    render(<ReadingFormatBadge format={format} />);
    expect(screen.getByText(t.reading.formatLabel[format])).toBeInTheDocument();
  });
});
