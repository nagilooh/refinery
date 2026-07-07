/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { Cite } from '@citation-js/core';
import '@citation-js/plugin-bibtex';
import bibtexParse from 'bibtex-parse-js';
import { useEffect, useState, type ComponentProps } from 'react';

import Publication from './Publication';

type EntryTags = Record<string, string>;

interface RawBibEntry {
  entryType: string;
  citationKey: string;
  entryTags: EntryTags;
}

interface CiteAuthor {
  given?: string;
  family?: string;
}

interface CiteEntry {
  author?: CiteAuthor[];
  title?: string;
  DOI?: string;
  URL?: string;
  abstract?: string;
  issued?: {
    'date-parts'?: number[][];
  };
  raw?: RawBibEntry;
  'container-title'?: string;
}

interface CiteInstance {
  data?: unknown[];
}

type CiteConstructor = new (input: string) => CiteInstance;

interface BibtexParseModule {
  toJSON(input: string): unknown;
}

function isNumberMatrix(value: unknown): value is number[][] {
  return (
    Array.isArray(value) &&
    value.every(
      (part) =>
        Array.isArray(part) &&
        part.every((valuePart) => typeof valuePart === 'number'),
    )
  );
}

function asRecord(value: unknown): Record<string, unknown> {
  if (typeof value === 'object' && value !== null) {
    return value as Record<string, unknown>;
  }
  return {};
}

function normalizeRawBibEntry(value: unknown): RawBibEntry {
  const raw = asRecord(value);
  const entryTagsUnknown = asRecord(raw['entryTags']);

  const entryTags: EntryTags = {};
  for (const [key, item] of Object.entries(entryTagsUnknown)) {
    if (typeof item === 'string') {
      entryTags[key] = item;
    }
  }

  const entryTypeValue = raw['entryType'];
  const citationKeyValue = raw['citationKey'];

  return {
    entryType: typeof entryTypeValue === 'string' ? entryTypeValue : 'misc',
    citationKey:
      typeof citationKeyValue === 'string' ? citationKeyValue : 'unknown',
    entryTags,
  };
}

function normalizeCiteEntry(value: unknown): CiteEntry {
  const entry = asRecord(value);

  const issuedRecord = asRecord(entry['issued']);
  const datePartsValue = issuedRecord['date-parts'];
  const dateParts = isNumberMatrix(datePartsValue) ? datePartsValue : undefined;

  const authorValue = entry['author'];
  const authors = Array.isArray(authorValue)
    ? authorValue
        .map((author): CiteAuthor | undefined => {
          const authorRecord = asRecord(author);
          const given = authorRecord['given'];
          const family = authorRecord['family'];

          const normalized: CiteAuthor = {};
          if (typeof given === 'string') {
            normalized.given = given;
          }
          if (typeof family === 'string') {
            normalized.family = family;
          }
          return normalized.given || normalized.family ? normalized : undefined;
        })
        .filter((author): author is CiteAuthor => author !== undefined)
    : undefined;

  const normalized: CiteEntry = {
    raw: normalizeRawBibEntry(entry['raw']),
  };

  if (authors && authors.length > 0) {
    normalized.author = authors;
  }

  const title = entry['title'];
  if (typeof title === 'string') {
    normalized.title = title;
  }

  const doi = entry['DOI'];
  if (typeof doi === 'string') {
    normalized.DOI = doi;
  }

  const url = entry['URL'];
  if (typeof url === 'string') {
    normalized.URL = url;
  }

  const abstract = entry['abstract'];
  if (typeof abstract === 'string') {
    normalized.abstract = abstract;
  }

  if (dateParts) {
    normalized.issued = { 'date-parts': dateParts };
  }

  const containerTitle = entry['container-title'];
  if (typeof containerTitle === 'string') {
    normalized['container-title'] = containerTitle;
  }

  return normalized;
}

function parseBibToEntries(bibText: string): CiteEntry[] {
  const TypedCite = Cite as unknown as CiteConstructor;
  const typedBibtexParse = bibtexParse as unknown as BibtexParseModule;

  const cite = new TypedCite(bibText);
  const parsedRawUnknown = typedBibtexParse.toJSON(bibText);
  const parsedRaw = Array.isArray(parsedRawUnknown) ? parsedRawUnknown : [];

  const data = Array.isArray(cite.data) ? cite.data : [];

  return data.map((item, index) => {
    const entry = normalizeCiteEntry(item);
    const rawFallback = normalizeRawBibEntry(parsedRaw[index]);

    return {
      ...entry,
      raw: entry.raw ?? rawFallback,
    };
  });
}

export default function BibPublications({ bib }: { bib: string }) {
  const [entries, setEntries] = useState<CiteEntry[]>([]);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    async function loadBib() {
      try {
        const response = await fetch(bib);

        if (!response.ok) {
          throw new Error(
            `Failed to fetch BibTeX file "${bib}": ${response.statusText}`,
          );
        }

        const bibText = await response.text();
        const parsedEntries = parseBibToEntries(bibText);
        setEntries(parsedEntries);
      } catch (err: unknown) {
        setError(err instanceof Error ? err : new Error(String(err)));
      }
    }

    void loadBib();
  }, [bib]);

  if (error) {
    return <div>{error.message}</div>;
  }

  return (
    <ul>
      {entries
        .sort((a, b) => (year(b) ?? 0) - (year(a) ?? 0))
        .map((entry, index) => (
          <div key={index} className="publication-entry">
            <Publication {...toPublicationProps(entry)} />
          </div>
        ))}
    </ul>
  );
}

function year(entry: CiteEntry): number | undefined {
  return entry.issued?.['date-parts']?.[0]?.[0];
}

function toPublicationProps(
  entry: CiteEntry,
): ComponentProps<typeof Publication> {
  const entryTags = entry.raw?.entryTags ?? {};
  const publicationProps: ComponentProps<typeof Publication> = {
    bibtex: entryToBibtex(entry),
  };

  const authors = entry.author
    ?.map((author) => ({
      given: author.given ?? '',
      family: author.family ?? '',
    }))
    .filter((author) => author.given !== '' || author.family !== '');
  if (authors && authors.length > 0) {
    publicationProps.authors = authors;
  }

  if (entry.title) {
    publicationProps.title = entry.title;
  }

  const publicationYear = year(entry);
  if (publicationYear !== undefined) {
    publicationProps.year = publicationYear;
  }

  if (entry.DOI) {
    publicationProps.doi = entry.DOI;
  }

  const venue = entry['container-title'] ?? entryTags['note'];
  if (venue) {
    publicationProps.venue = venue;
  }

  const links = [
    ...(entry.URL ? [{ label: 'link', url: entry.URL }] : []),
    ...Object.entries(entryTags)
      .filter(([key]) => key.startsWith('url_'))
      .map(([key, value]) => ({
        label: key.replace(/^url_/, ''),
        url: value,
      })),
  ].filter(
    (value, index, self) =>
      index === self.findIndex((current) => current.url === value.url),
  );

  if (links.length > 0) {
    publicationProps.links = links;
  }

  const link = entry.URL ?? entryTags['url'] ?? entryTags['url_link'];
  if (link) {
    publicationProps.link = link;
  }

  if (entryTags['url_pdf']) {
    publicationProps.pdf = entryTags['url_pdf'];
  }
  if (entryTags['url_slides']) {
    publicationProps.slides = entryTags['url_slides'];
  }
  if (entryTags['url_video']) {
    publicationProps.video = entryTags['url_video'];
  }

  if (entry.abstract) {
    publicationProps.abstract = entry.abstract;
  }

  return publicationProps;
}

function entryToBibtex(entry: CiteEntry): string {
  const raw = entry.raw ?? {
    entryType: 'misc',
    citationKey: 'unknown',
    entryTags: {},
  };

  const keys = Object.keys(raw.entryTags);
  const maxKeyLength =
    keys.length > 0 ? Math.max(...keys.map((key) => key.length)) : 0;

  return `@${raw.entryType}{${raw.citationKey},
${Object.entries(raw.entryTags)
  .map(([key, value]) => `  ${key.padEnd(maxKeyLength)} = {${value}}`)
  .join(',\n')}
}`;
}
