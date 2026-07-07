/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Link from '@docusaurus/Link';
import React, { useEffect, useId, useRef, useState } from 'react';

import styles from './Publication.module.css';

const specialAbbreviations: Record<string, string> = {
  Csanád: 'Cs.',
};

interface PublicationLink {
  label: string;
  url: string;
}

interface PublicationProps {
  authors?: { given: string; family: string }[];
  title?: string;
  venue?: string;
  year?: number;
  doi?: string;
  pdf?: string;
  links?: PublicationLink[];
  abstract?: string;
  bibtex?: string;
  children?: React.ReactNode;
  newLineBeforeLinks?: boolean;
  video?: string;
  slides?: string;
  link?: string;
}

export default function Publication({
  authors,
  title,
  venue,
  year,
  doi,
  pdf,
  links,
  abstract,
  bibtex,
  children,
  newLineBeforeLinks = true,
  video,
  slides,
  link,
}: PublicationProps) {
  const [openSections, setOpenSections] = useState({
    bibtex: false,
    abstract: false,
  });
  const [copiedSection, setCopiedSection] = useState<
    'bibtex' | 'abstract' | null
  >(null);
  const bibtexId = useId();
  const abstractId = useId();
  const copiedTimeoutRef = useRef<number | null>(null);
  const doiLink = doi ? `https://doi.org/${doi}` : '';

  useEffect(() => {
    return () => {
      if (copiedTimeoutRef.current !== null) {
        window.clearTimeout(copiedTimeoutRef.current);
      }
    };
  }, []);

  function toggleSection(section: 'bibtex' | 'abstract') {
    setOpenSections((current) => ({
      ...current,
      [section]: !current[section],
    }));
  }

  async function copySection(section: 'bibtex' | 'abstract', text: string) {
    await navigator.clipboard.writeText(text);
    setCopiedSection(section);

    if (copiedTimeoutRef.current !== null) {
      window.clearTimeout(copiedTimeoutRef.current);
    }

    copiedTimeoutRef.current = window.setTimeout(() => {
      setCopiedSection((current) => (current === section ? null : current));
      copiedTimeoutRef.current = null;
    }, 1500);
  }

  const hasLinks =
    !!doi ||
    (links?.length ?? 0) > 0 ||
    !!link ||
    !!pdf ||
    !!slides ||
    !!video ||
    !!abstract ||
    !!bibtex;

  return (
    <li className={styles['entry']}>
      <div className={styles['summary']}>
        {children}
        {authors && <>{authors.map(abbreviateAuthor).join(', ')}:</>}
        {title && (
          <>
            {' '}
            <em>{title}.</em>
          </>
        )}
        {venue && <> {venue}</>}
        {year !== undefined && <> ({year})</>}

        {newLineBeforeLinks && hasLinks && (
          <>
            <br />
          </>
        )}

        {doi && (
          <>
            {' '}
            [<Link href={doiLink}>doi</Link>]
          </>
        )}

        {links?.map((item) => {
          if (item.url !== doiLink) {
            return (
              <React.Fragment key={item.label}>
                {' '}
                [<Link href={prepareLink(item.url)}>{item.label}</Link>]
              </React.Fragment>
            );
          }
          return null;
        })}

        {link && link !== doiLink && (
          <>
            {' '}
            [<Link href={prepareLink(link)}>link</Link>]
          </>
        )}
        {pdf && (
          <>
            {' '}
            [<Link href={prepareLink(pdf)}>pdf</Link>]
          </>
        )}
        {slides && (
          <>
            {' '}
            [<Link href={prepareLink(slides)}>slides</Link>]
          </>
        )}
        {video && (
          <>
            {' '}
            [<Link href={prepareLink(video)}>video</Link>]
          </>
        )}

        {abstract && (
          <>
            {' '}
            [
            <Link
              href="#"
              aria-expanded={openSections.abstract}
              aria-controls={abstractId}
              onClick={(event) => {
                event.preventDefault();
                toggleSection('abstract');
              }}
            >
              abstract {openSections.abstract ? '▴' : '▾'}
            </Link>
            ]
          </>
        )}
        {bibtex && (
          <>
            {' '}
            [
            <Link
              href="#"
              aria-expanded={openSections.bibtex}
              aria-controls={bibtexId}
              onClick={(event) => {
                event.preventDefault();
                toggleSection('bibtex');
              }}
            >
              bibtex {openSections.bibtex ? '▴' : '▾'}
            </Link>
            ]
          </>
        )}
      </div>

      {abstract && (
        <div
          className={`${styles['panel']} ${
            openSections.abstract ? styles['panel--open'] : ''
          }`}
          id={abstractId}
          aria-hidden={!openSections.abstract}
        >
          <button
            type="button"
            className={styles['copyButton']}
            onClick={() => {
              void copySection('abstract', abstract);
            }}
          >
            {copiedSection === 'abstract' ? 'Copied' : 'Copy'}
          </button>
          <div className={styles['panel-inner']}>
            <pre className={styles['content']}>{abstract}</pre>
          </div>
        </div>
      )}

      {bibtex && (
        <div
          className={`${styles['panel']} ${
            openSections.bibtex ? styles['panel--open'] : ''
          }`}
          id={bibtexId}
          aria-hidden={!openSections.bibtex}
        >
          <button
            type="button"
            className={styles['copyButton']}
            onClick={() => {
              void copySection('bibtex', bibtex);
            }}
          >
            {copiedSection === 'bibtex' ? 'Copied' : 'Copy'}
          </button>
          <div className={styles['panel-inner']}>
            <pre className={`${styles['content']} ${styles['mono']}`}>
              {bibtex}
            </pre>
          </div>
        </div>
      )}
    </li>
  );
}

function prepareLink(url: string): string {
  if (url.startsWith('/')) {
    return `pathname://${url}`;
  }
  return url;
}

function abbreviateAuthor(author: { given: string; family: string }): string {
  if (specialAbbreviations[author.given]) {
    return `${specialAbbreviations[author.given]} ${author.family}`;
  }

  if (author.given.length === 0) {
    return author.family;
  }

  return `${author.given.charAt(0)}. ${author.family}`;
}
