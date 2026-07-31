import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { createDiscountCode, deleteDiscountCode, findDiscountCodes, updateDiscountCode } from '../api/discountCodeApi';
import type { CreateDiscountCodeRequest, DiscountCode } from '../api/discountCodeApi';
import { useAuth } from '../auth/useAuth';
import { StatusBadge } from '../components/StatusBadge';

export function DiscountCodesPage({ hotelId }: { hotelId: string }) {
  const { session } = useAuth();
  const [codes, setCodes] = useState<DiscountCode[]>([]);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingCodeId, setEditingCodeId] = useState<string | null>(null);
  const [code, setCode] = useState('');
  const [discountType, setDiscountType] = useState<'percentage' | 'fixedAmount'>('percentage');
  const [discountValue, setDiscountValue] = useState('');
  const [validFrom, setValidFrom] = useState(today());
  const [validUntil, setValidUntil] = useState('');
  const [maximumUses, setMaximumUses] = useState('');
  const [error, setError] = useState('');

  const loadCodes = useCallback(async () => {
    if (!session || !hotelId) {
      setCodes([]);
      return;
    }

    try {
      setCodes(await findDiscountCodes(session.accessToken, hotelId));
      setError('');
    } catch {
      setError('Discount codes could not be loaded. Please try again.');
    }
  }, [hotelId, session]);

  useEffect(() => {
    void loadCodes();
  }, [loadCodes]);

  function resetForm() {
    setIsFormOpen(false);
    setCode('');
    setDiscountType('percentage');
    setDiscountValue('');
    setValidFrom(today());
    setValidUntil('');
    setMaximumUses('');
    setEditingCodeId(null);
  }

  function edit(discountCode: DiscountCode) {
    setEditingCodeId(discountCode.id);
    setCode(discountCode.code);
    setDiscountType(discountCode.percentage !== null ? 'percentage' : 'fixedAmount');
    setDiscountValue(String(discountCode.percentage ?? discountCode.fixedAmount ?? ''));
    setValidFrom(discountCode.validFrom);
    setValidUntil(discountCode.validUntil);
    setMaximumUses(discountCode.maximumUses?.toString() ?? '');
    setIsFormOpen(true);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session) {
      return;
    }

    const request: CreateDiscountCodeRequest = {
      hotelId,
      code: code.trim(),
      percentage: discountType === 'percentage' ? Number(discountValue) : null,
      fixedAmount: discountType === 'fixedAmount' ? Number(discountValue) : null,
      validFrom,
      validUntil,
      maximumUses: maximumUses ? Number(maximumUses) : null,
    };

    try {
      if (editingCodeId) await updateDiscountCode(session.accessToken, editingCodeId, request);
      else await createDiscountCode(session.accessToken, request);
      resetForm();
      await loadCodes();
    } catch {
      setError('The discount code could not be created. Check its value and validity dates, then try again.');
    }
  }

  async function deleteCode(id: string) {
    if (!session) {
      return;
    }

    if (!window.confirm('Delete this discount code? It will be removed from normal management screens.')) {
      return;
    }

    try {
      await deleteDiscountCode(session.accessToken, id);
      await loadCodes();
    } catch {
      setError('The discount code could not be deleted. Please try again.');
    }
  }

  return <section>
    <div className="page-heading"><div><p className="eyebrow">Pricing</p><h1>Discount codes</h1><p>Create promotions that guests can apply while booking.</p></div><button type="button" disabled={!hotelId} onClick={() => setIsFormOpen(true)}>New discount code</button></div>
    {error && <p className="form-error" role="alert">{error}</p>}
    {!hotelId && <div className="empty-state"><h2>Select a hotel first</h2><p>Discount codes belong to one hotel.</p></div>}
    {hotelId && codes.length === 0 && !error && <div className="empty-state"><h2>No discount codes yet</h2><p>Create one when you want to run a promotion.</p></div>}
    {codes.length > 0 && <div className="table-container"><table><thead><tr><th>Code</th><th>Discount</th><th>Validity</th><th>Uses</th><th>Status</th><th /></tr></thead><tbody>{codes.map((discountCode) => <tr key={discountCode.id}><td><strong>{discountCode.code}</strong></td><td>{formatDiscount(discountCode)}</td><td>{discountCode.validFrom} to {discountCode.validUntil}</td><td>{discountCode.usedCount} / {discountCode.maximumUses ?? 'Unlimited'}</td><td><StatusBadge label={discountCode.active ? 'Active' : 'Inactive'} tone={discountCode.active ? 'positive' : 'negative'} /></td><td><button type="button" className="secondary-button" onClick={() => edit(discountCode)}>Edit</button> <button type="button" className="secondary-button" onClick={() => void deleteCode(discountCode.id)}>Delete</button></td></tr>)}</tbody></table></div>}
    {isFormOpen && <section className="setup-card room-type-form-card"><h2>New discount code</h2><form className="hotel-form" onSubmit={handleSubmit}><label>Code<input value={code} onChange={(event) => setCode(event.target.value.toUpperCase())} placeholder="SUMMER10" required /></label><label>Discount type<select value={discountType} onChange={(event) => setDiscountType(event.target.value as 'percentage' | 'fixedAmount')}><option value="percentage">Percentage</option><option value="fixedAmount">Fixed amount (€)</option></select></label><label>{discountType === 'percentage' ? 'Percentage' : 'Amount (€)'}<input type="number" min="0.01" max={discountType === 'percentage' ? '100' : undefined} step="0.01" value={discountValue} onChange={(event) => setDiscountValue(event.target.value)} required /></label><label>Maximum uses <span className="field-help">Leave empty for unlimited use.</span><input type="number" min="1" step="1" value={maximumUses} onChange={(event) => setMaximumUses(event.target.value)} /></label><label>Valid from<input type="date" value={validFrom} onChange={(event) => setValidFrom(event.target.value)} required /></label><label>Valid until<input type="date" value={validUntil} onChange={(event) => setValidUntil(event.target.value)} required /></label><div className="form-actions full-width"><button type="button" className="secondary-button" onClick={resetForm}>Cancel</button><button type="submit">Create discount code</button></div></form></section>}
  </section>;
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

function formatDiscount(discountCode: DiscountCode) {
  if (discountCode.percentage !== null) {
    return `${discountCode.percentage}%`;
  }
  if (discountCode.fixedAmount === null) {
    return '—';
  }

  return new Intl.NumberFormat('en-GB', { style: 'currency', currency: 'EUR' }).format(discountCode.fixedAmount);
}
