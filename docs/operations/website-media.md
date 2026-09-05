# Website media

Managers can upload a homepage hero and hotel gallery from **Hotel settings**.
Room-type galleries are managed through the **Photos** action on **Room types**.
The first room photo is used as its cover and all uploaded photos appear on the
public room detail page. Built-in images remain as fallbacks where no custom image
exists.

Uploads are hotel-scoped and require the corresponding management permission.
Only validated JPEG and PNG images up to 8 MB and 40 megapixels are accepted.
Alternative text should describe meaningful visual content for screen-reader users.

Metadata is stored in the hotel-service PostgreSQL database. Local image files are
stored in the `hotel-booking-website-media` Docker volume mounted at
`/data/website-media`. Database backups alone therefore do not contain uploaded
photos: back up this volume as well before moving or destroying an environment.
Deleting the volume permanently removes those files.

`WebsiteMediaStorage` is the storage boundary. A future AWS deployment should add
an S3 implementation behind that boundary and serve immutable objects through
CloudFront or signed/public object URLs, without changing hotel ownership metadata
or the admin workflow.
