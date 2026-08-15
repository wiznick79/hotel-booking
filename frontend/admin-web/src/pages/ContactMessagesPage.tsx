import { useCallback, useEffect, useState } from 'react';
import {
  findContactMessages,
  markContactMessageRead,
  type ContactMessage,
  type ContactMessagePage,
} from '../api/contactMessageApi';
import { useAuth } from '../auth/useAuth';
import { StatusBadge } from '../components/StatusBadge';
import { translateAdmin, type AdminLanguage } from '../i18n';

type ContactMessagesPageProps = {
  hotelId: string;
  language: AdminLanguage;
};

const emptyPage: ContactMessagePage = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
};

export function ContactMessagesPage({ hotelId, language }: ContactMessagesPageProps) {
  const { session } = useAuth();
  const [messages, setMessages] = useState<ContactMessagePage>(emptyPage);
  const [selectedMessage, setSelectedMessage] = useState<ContactMessage | null>(null);
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const t = (key: Parameters<typeof translateAdmin>[1]) => translateAdmin(language, key);

  const load = useCallback(async () => {
    if (!session || !hotelId) {
      setMessages(emptyPage);
      return;
    }

    setLoading(true);

    try {
      setMessages(await findContactMessages(session.accessToken, hotelId, page));
      setError('');
    } catch {
      setError(t('contactMessagesLoadFailed'));
    } finally {
      setLoading(false);
    }
  }, [hotelId, page, session, language]);

  useEffect(() => {
    setPage(0);
    setSelectedMessage(null);
  }, [hotelId]);

  useEffect(() => {
    void load();
  }, [load]);

  async function viewMessage(message: ContactMessage) {
    setSelectedMessage(message);

    if (!session || message.readAt) {
      return;
    }

    try {
      await markContactMessageRead(session.accessToken, message.id);
      const readAt = new Date().toISOString();
      setSelectedMessage({ ...message, readAt });
      setMessages((current) => ({
        ...current,
        content: current.content.map((candidate) => candidate.id === message.id
          ? { ...candidate, readAt }
          : candidate),
      }));
    } catch {
      setError(t('contactMessageReadFailed'));
    }
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="eyebrow">{t('communication')}</p>
          <h1>{t('contactMessages')}</h1>
          <p>{t('contactMessagesDescription')}</p>
        </div>
      </div>

      {error && <p className="form-error">{error}</p>}

      {!error && !loading && messages.content.length === 0 && (
        <section className="empty-state">
          <h2>{t('noContactMessages')}</h2>
          <p>{t('noContactMessagesDescription')}</p>
        </section>
      )}

      {(loading || messages.content.length > 0) && (
        <div className="table-container contact-message-table">
          <table>
            <thead>
              <tr>
                <th>{t('received')}</th>
                <th>{t('sender')}</th>
                <th>{t('subject')}</th>
                <th>{t('delivery')}</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr>
                  <td colSpan={5}>{t('loading')}</td>
                </tr>
              )}
              {!loading && messages.content.map((message) => (
                <tr className={message.readAt ? '' : 'unread-message'} key={message.id}>
                  <td>{formatDate(message.receivedAt, language)}</td>
                  <td>
                    <strong>{message.name}</strong>
                    <span className="table-subtext">{message.email}</span>
                  </td>
                  <td>{message.subject}</td>
                  <td><DeliveryStatus message={message} /></td>
                  <td>
                    <button
                      className="secondary-button"
                      onClick={() => void viewMessage(message)}
                      type="button"
                    >
                      {t('view')}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {messages.totalPages > 1 && (
        <div className="pagination-bar">
          <button
            className="secondary-button"
            disabled={page === 0}
            onClick={() => setPage((current) => current - 1)}
            type="button"
          >
            {t('previous')}
          </button>
          <span>{t('page')} {page + 1} / {messages.totalPages}</span>
          <button
            className="secondary-button"
            disabled={page + 1 >= messages.totalPages}
            onClick={() => setPage((current) => current + 1)}
            type="button"
          >
            {t('next')}
          </button>
        </div>
      )}

      {selectedMessage && (
        <MessageDetails
          language={language}
          message={selectedMessage}
          onClose={() => setSelectedMessage(null)}
        />
      )}
    </section>
  );
}

function DeliveryStatus({ message }: { message: ContactMessage }) {
  const tone = message.deliveryStatus === 'SENT'
    ? 'positive'
    : message.deliveryStatus === 'FAILED'
      ? 'negative'
      : 'warning';

  return <StatusBadge label={message.deliveryStatus} tone={tone} />;
}

function MessageDetails({
  message,
  language,
  onClose,
}: {
  message: ContactMessage;
  language: AdminLanguage;
  onClose: () => void;
}) {
  const t = (key: Parameters<typeof translateAdmin>[1]) => translateAdmin(language, key);

  return (
    <section className="setup-card contact-message-detail">
      <div className="page-heading compact-heading">
        <div>
          <p className="eyebrow">{t('messageDetails')}</p>
          <h2>{message.subject}</h2>
        </div>
        <button className="secondary-button" onClick={onClose} type="button">{t('close')}</button>
      </div>

      <dl className="reservation-details">
        <div><dt>{t('sender')}</dt><dd>{message.name}</dd></div>
        <div><dt>{t('emailAddress')}</dt><dd><a href={`mailto:${message.email}`}>{message.email}</a></dd></div>
        <div><dt>{t('phone')}</dt><dd>{message.phone || t('notProvided')}</dd></div>
        <div><dt>{t('received')}</dt><dd>{formatDate(message.receivedAt, language)}</dd></div>
        <div><dt>{t('delivery')}</dt><dd><DeliveryStatus message={message} /></dd></div>
        <div><dt>{t('reference')}</dt><dd>{message.referenceId}</dd></div>
      </dl>

      <div className="contact-message-body">
        <h3>{t('message')}</h3>
        <p>{message.message}</p>
      </div>
    </section>
  );
}

function formatDate(value: string, language: AdminLanguage) {
  return new Intl.DateTimeFormat(language === 'pt' ? 'pt-PT' : 'en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}
