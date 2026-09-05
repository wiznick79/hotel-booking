import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { deleteWebsiteMedia, findWebsiteMedia, uploadWebsiteMedia } from '../api/mediaApi';
import type { WebsiteMedia, WebsiteMediaUsage } from '../api/mediaApi';
import { useAuth } from '../auth/useAuth';

export function WebsiteMediaManager({ hotelId, usage, roomTypeId, title, description }: {
  hotelId: string; usage: WebsiteMediaUsage; roomTypeId?: string;
  title: string; description: string;
}) {
  const { session } = useAuth();
  const [media, setMedia] = useState<WebsiteMedia[]>([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function load() {
    if (!session || !hotelId) return;
    try {
      const all = await findWebsiteMedia(session.accessToken, hotelId);
      setMedia(all.filter(item => item.usage === usage && (roomTypeId ? item.roomTypeId === roomTypeId : !item.roomTypeId)));
      setError('');
    } catch { setError('Images could not be loaded.'); }
  }

  useEffect(() => { void load(); }, [hotelId, roomTypeId, session]);

  async function upload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session) return;
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const file = form.get('file');
    if (!(file instanceof File) || file.size === 0) { setError('Choose a JPEG or PNG image.'); return; }
    setBusy(true);
    try {
      await uploadWebsiteMedia(session.accessToken, {
        hotelId, roomTypeId, usage, file,
        altText: String(form.get('altText') ?? '').trim(),
        sortOrder: usage === 'HERO' ? 0 : media.length,
      });
      formElement.reset();
      await load();
    } catch (exception) { setError(exception instanceof Error ? exception.message : 'The image could not be uploaded.'); }
    finally { setBusy(false); }
  }

  async function remove(item: WebsiteMedia) {
    if (!session || !window.confirm(`Delete ${item.originalFilename}?`)) return;
    try { await deleteWebsiteMedia(session.accessToken, item.id); await load(); }
    catch { setError('The image could not be deleted.'); }
  }

  return <section className="media-manager">
    <div><h3>{title}</h3><p>{description}</p></div>
    {error && <p className="form-error" role="alert">{error}</p>}
    {media.length > 0 && <div className="media-grid">{media.map(item => <figure key={item.id}>
      <img src={item.url} alt={item.altText ?? ''} />
      <figcaption><span>{item.altText || item.originalFilename}</span>
        <button className="text-button media-delete" type="button" onClick={() => void remove(item)}>Delete</button>
      </figcaption>
    </figure>)}</div>}
    <form className="media-upload-form" onSubmit={upload}>
      <label>Photo<input name="file" type="file" accept="image/jpeg,image/png" required /></label>
      <label>Alternative text<input name="altText" maxLength={255} placeholder="Describe the photo for screen readers" /></label>
      <button type="submit" disabled={busy}>{busy ? 'Uploading…' : usage === 'HERO' && media.length ? 'Replace photo' : 'Upload photo'}</button>
    </form>
    <small>JPEG or PNG, up to 8 MB. Uploading a new hero replaces the current one.</small>
  </section>;
}
