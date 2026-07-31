import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { createPricingRule, deactivatePricingRule, findPricingRules, updatePricingRule } from '../api/rateApi';
import type { CreatePricingRuleRequest, PricingRule, PricingRuleType } from '../api/rateApi';
import { findRoomTypes } from '../api/roomApi';
import type { RoomType } from '../api/roomApi';
import { useAuth } from '../auth/useAuth';

type PriceDraft = {
  normalNightlyPrice: string;
  weekendNightlyPrice: string;
};

export function RatesPage({ hotelId }: { hotelId: string }) {
  const { session } = useAuth();
  const [roomTypes, setRoomTypes] = useState<RoomType[]>([]);
  const [pricingRules, setPricingRules] = useState<PricingRule[]>([]);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingRuleId, setEditingRuleId] = useState<string | null>(null);
  const [name, setName] = useState('');
  const [ruleType, setRuleType] = useState<PricingRuleType>('RECURRING_SEASON');
  const [startMonth, setStartMonth] = useState('');
  const [startDay, setStartDay] = useState('');
  const [endMonth, setEndMonth] = useState('');
  const [endDay, setEndDay] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [priority, setPriority] = useState('0');
  const [priceDrafts, setPriceDrafts] = useState<Record<string, PriceDraft>>({});
  const [error, setError] = useState('');

  const loadPricing = useCallback(async () => {
    if (!hotelId || !session) {
      setRoomTypes([]);
      setPricingRules([]);
      return;
    }

    try {
      const selectedRoomTypes = (await findRoomTypes(session.accessToken))
        .filter((roomType) => roomType.hotelId === hotelId);
      const rules = await findPricingRules(session.accessToken, hotelId);
      setRoomTypes(selectedRoomTypes);
      setPricingRules(rules);
      setPriceDrafts((current) => Object.fromEntries(selectedRoomTypes.map((roomType) => [roomType.id,
        current[roomType.id] ?? defaultPriceDraft(roomType)] )));
      setError('');
    } catch {
      setError('Pricing rules could not be loaded. Please try again.');
    }
  }, [hotelId, session]);

  useEffect(() => {
    void loadPricing();
  }, [loadPricing]);

  function updatePrice(roomTypeId: string, field: keyof PriceDraft, value: string) {
    setPriceDrafts((current) => ({
      ...current,
      [roomTypeId]: {
        ...(current[roomTypeId] ?? { normalNightlyPrice: '', weekendNightlyPrice: '' }),
        [field]: value,
      },
    }));
  }

  function resetForm() {
    setIsFormOpen(false);
    setName('');
    setStartMonth('');
    setStartDay('');
    setEndMonth('');
    setEndDay('');
    setStartDate('');
    setEndDate('');
    setPriority('0');
    setPriceDrafts(Object.fromEntries(roomTypes.map((roomType) => [roomType.id, defaultPriceDraft(roomType)])));
    setEditingRuleId(null);
  }

  function editRule(rule: PricingRule) {
    setEditingRuleId(rule.id); setName(rule.name); setRuleType(rule.ruleType);
    setStartMonth(rule.recurringStartMonth?.toString() ?? ''); setStartDay(rule.recurringStartDay?.toString() ?? '');
    setEndMonth(rule.recurringEndMonth?.toString() ?? ''); setEndDay(rule.recurringEndDay?.toString() ?? '');
    setStartDate(rule.startDate ?? ''); setEndDate(rule.endDate ?? ''); setPriority(String(rule.priority));
    setPriceDrafts(Object.fromEntries(roomTypes.map((roomType) => {
      const price = rule.roomTypePrices.find((entry) => entry.roomTypeId === roomType.id);
      return [roomType.id, price ? { normalNightlyPrice: String(price.normalNightlyPrice), weekendNightlyPrice: price.weekendNightlyPrice?.toString() ?? '' } : { normalNightlyPrice: '', weekendNightlyPrice: '' }];
    })));
    setIsFormOpen(true);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session) {
      return;
    }

    const roomTypePrices = roomTypes
      .filter((roomType) => priceDrafts[roomType.id]?.normalNightlyPrice.trim())
      .map((roomType) => ({
        roomTypeId: roomType.id,
        normalNightlyPrice: Number(priceDrafts[roomType.id].normalNightlyPrice),
        weekendNightlyPrice: priceDrafts[roomType.id].weekendNightlyPrice.trim()
          ? Number(priceDrafts[roomType.id].weekendNightlyPrice)
          : null,
      }));

    if (!roomTypePrices.length) {
      setError('Enter a normal nightly price for at least one room type.');
      return;
    }

    const request: CreatePricingRuleRequest = {
      hotelId,
      name: name.trim(),
      ruleType,
      recurringStartMonth: ruleType === 'RECURRING_SEASON' ? Number(startMonth) : null,
      recurringStartDay: ruleType === 'RECURRING_SEASON' ? Number(startDay) : null,
      recurringEndMonth: ruleType === 'RECURRING_SEASON' ? Number(endMonth) : null,
      recurringEndDay: ruleType === 'RECURRING_SEASON' ? Number(endDay) : null,
      startDate: ruleType === 'DATE_OVERRIDE' ? startDate : null,
      endDate: ruleType === 'DATE_OVERRIDE' ? endDate : null,
      priority: Number(priority),
      roomTypePrices,
    };

    try {
      if (editingRuleId) await updatePricingRule(session.accessToken, editingRuleId, request);
      else await createPricingRule(session.accessToken, request);
      resetForm();
      await loadPricing();
    } catch {
      setError('The pricing rule could not be created. Check the dates and prices, then try again.');
    }
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Pricing</p>
          <h1>Rates</h1>
          <p>Rules apply to selected room types. Friday and Saturday use the optional weekend price.</p>
        </div>
        <button type="button" disabled={!roomTypes.length} onClick={() => { resetForm(); setIsFormOpen(true); }}>New pricing rule</button>
      </div>

      {error && <p className="form-error" role="alert">{error}</p>}
      {!roomTypes.length && !error && <div className="empty-state"><h2>Create a room type first</h2><p>Pricing rules set rates for one or more room types.</p></div>}
      {pricingRules.length > 0 && <div className="table-container"><table><thead><tr><th>Name</th><th>Period</th><th>Priority</th><th>Room type prices</th><th /></tr></thead><tbody>{pricingRules.map((rule) => <tr key={rule.id}><td><strong>{rule.name}</strong></td><td>{formatPeriod(rule)}</td><td>{rule.priority}</td><td>{rule.roomTypePrices.map((price) => <div key={price.roomTypeId}>{roomTypes.find((roomType) => roomType.id === price.roomTypeId)?.name}: {formatPrice(price.normalNightlyPrice)}{price.weekendNightlyPrice !== null ? ` / ${formatPrice(price.weekendNightlyPrice)}` : ''}</div>)}</td><td>{rule.active && <><button type="button" className="secondary-button" onClick={() => editRule(rule)}>Edit</button> <button type="button" className="secondary-button" onClick={() => { if (session) void deactivatePricingRule(session.accessToken, rule.id).then(loadPricing).catch(() => setError('The pricing rule could not be deactivated.')); }}>Deactivate</button></>}</td></tr>)}</tbody></table></div>}

      {isFormOpen && <section className="setup-card room-type-form-card"><h2>New pricing rule</h2><form className="hotel-form" onSubmit={handleSubmit}><label>Name<input value={name} onChange={(event) => setName(event.target.value)} placeholder="Christmas season" required /></label><label>Rule type<select value={ruleType} onChange={(event) => setRuleType(event.target.value as PricingRuleType)}><option value="RECURRING_SEASON">Recurring season</option><option value="DATE_OVERRIDE">One-off date override</option></select></label>{ruleType === 'RECURRING_SEASON' ? <div className="annual-date-range full-width"><fieldset><legend>Starts every year</legend><AnnualDatePicker month={startMonth} day={startDay} onMonthChange={setStartMonth} onDayChange={setStartDay} /></fieldset><fieldset><legend>Ends every year</legend><AnnualDatePicker month={endMonth} day={endDay} onMonthChange={setEndMonth} onDayChange={setEndDay} /></fieldset></div> : <><label>Start date<input type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} required /></label><label>End date<input type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} required /></label></>}<label>Priority<input type="number" min="0" value={priority} onChange={(event) => setPriority(event.target.value)} required /><span className="field-help">Higher numbers override lower-priority rules. A date override wins a tie.</span></label><div className="full-width"><h3>Room type prices</h3><p>Normal prices start with each room type’s base price. Clear a row only when this rule should not affect that room type.</p>{roomTypes.map((roomType) => <div className="price-row" key={roomType.id}><strong>{roomType.name}</strong><label>Normal night<input type="number" min="0" step="0.01" value={priceDrafts[roomType.id]?.normalNightlyPrice ?? ''} onChange={(event) => updatePrice(roomType.id, 'normalNightlyPrice', event.target.value)} /></label><label>Friday/Saturday<input type="number" min="0" step="0.01" value={priceDrafts[roomType.id]?.weekendNightlyPrice ?? ''} onChange={(event) => updatePrice(roomType.id, 'weekendNightlyPrice', event.target.value)} /></label></div>)}</div><div className="form-actions full-width"><button type="button" className="secondary-button" onClick={resetForm}>Cancel</button><button type="submit">Create pricing rule</button></div></form></section>}
    </section>
  );
}

function formatPeriod(rule: PricingRule) {
  if (rule.ruleType === 'DATE_OVERRIDE') {
    return `${rule.startDate} to ${rule.endDate}`;
  }
  return `Every year: ${rule.recurringStartDay}/${rule.recurringStartMonth} to ${rule.recurringEndDay}/${rule.recurringEndMonth}`;
}

function formatPrice(value: number) {
  return new Intl.NumberFormat('en-GB', { style: 'currency', currency: 'EUR' }).format(value);
}

function AnnualDatePicker({ month, day, onMonthChange, onDayChange }: {
  month: string;
  day: string;
  onMonthChange: (value: string) => void;
  onDayChange: (value: string) => void;
}) {
  const numberOfDays = month ? new Date(2028, Number(month), 0).getDate() : 0;

  function handleMonthChange(value: string) {
    onMonthChange(value);
    if (day && Number(day) > new Date(2028, Number(value), 0).getDate()) {
      onDayChange('');
    }
  }

  return <div className="annual-date-picker"><label>Month<select value={month} onChange={(event) => handleMonthChange(event.target.value)} required><option value="">Month</option>{monthNames.map((name, index) => <option key={name} value={index + 1}>{name}</option>)}</select></label><label>Day<select value={day} onChange={(event) => onDayChange(event.target.value)} disabled={!month} required><option value="">Day</option>{Array.from({ length: numberOfDays }, (_, index) => index + 1).map((value) => <option key={value} value={value}>{value}</option>)}</select></label></div>;
}

function defaultPriceDraft(roomType: RoomType): PriceDraft {
  return { normalNightlyPrice: String(roomType.basePrice), weekendNightlyPrice: '' };
}

const monthNames = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];
