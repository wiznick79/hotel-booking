# Internationalization

## Content translations

Business content such as hotel names, room-type names, and descriptions is stored in translation records rather than language-specific columns.

Each translation uses a standard language code such as `en`, `pt-PT`, `es`, `fr`, or `de`. A unique constraint prevents duplicate translations for the same entity and language.

## Fallback

The default content language is English and is configured rather than hardcoded. When the requested language is unavailable, the API falls back to English.

The requested language may come from an explicit API parameter or the HTTP `Accept-Language` header. Frontend interface translations are separate from backend-managed hotel content.

## Initial implementation scope

The first translated domain object will be `RoomType`. We will avoid a generic translation interface until a second translated domain object demonstrates that the abstraction is useful.
