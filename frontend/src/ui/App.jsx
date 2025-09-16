import React, { useEffect, useState } from 'react'

const API = '/api'

export default function App() {
  const [invoices, setInvoices] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [customerName, setCustomerName] = useState('')
  const [search, setSearch] = useState('')
  const [newItemDesc, setNewItemDesc] = useState('')
  const [newItemPrice, setNewItemPrice] = useState('')
  const [newItems, setNewItems] = useState([])
  const [rowItems, setRowItems] = useState({}) // { [invoiceId]: { desc, price } }
  const [detailsOpen, setDetailsOpen] = useState(null) // invoice or null
  const [detailsItems, setDetailsItems] = useState([])

  async function fetchInvoices() {
    setLoading(true)
    setError('')
    try {
      const res = await fetch(`${API}/invoices`)
      const data = await res.json()
      setInvoices(data)
    } catch (e) {
      setError('Failed to load invoices')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchInvoices()
  }, [])

  function openDetails(inv) {
    setDetailsOpen(inv)
    setDetailsItems((inv.items || []).map(it => ({ description: it.description || '', price: it.price ?? '' })))
  }

  function closeDetails() {
    setDetailsOpen(null)
    setDetailsItems([])
  }

  function updateDetailsItem(index, field, value) {
    setDetailsItems(items => items.map((it, i) => i === index ? { ...it, [field]: value } : it))
  }

  function addDetailsItem() {
    setDetailsItems(items => [...items, { description: '', price: '' }])
  }

  function removeDetailsItem(index) {
    setDetailsItems(items => items.filter((_, i) => i !== index))
  }

  async function saveDetails() {
    if (!detailsOpen) return
    const payloadItems = []
    for (const it of detailsItems) {
      const desc = String(it.description || '').trim()
      const priceNum = parseFloat(String(it.price).replace(',', '.'))
      if (!desc) continue
      if (Number.isNaN(priceNum) || !Number.isFinite(priceNum) || priceNum < 0) continue
      payloadItems.push({ description: desc, price: priceNum })
    }
    try {
      const res = await fetch(`${API}/invoices/${detailsOpen.id}/items`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ items: payloadItems })
      })
      if (!res.ok) throw new Error()
      closeDetails()
      fetchInvoices()
    } catch {
      setError('Failed to save changes')
    }
  }

  function addItem(e) {
    e.preventDefault()
    const desc = newItemDesc.trim()
    const priceNum = parseFloat(String(newItemPrice).replace(',', '.'))
    if (!desc) return
    if (Number.isNaN(priceNum) || !Number.isFinite(priceNum) || priceNum < 0) return
    setNewItems(items => [...items, { 
      id: Date.now() + Math.random().toString(36).substr(2, 9),
      description: desc, 
      price: priceNum 
    }])
    setNewItemDesc('')
    setNewItemPrice('')
  }

  function removeItem(id) {
    setNewItems(items => items.filter(item => item.id !== id))
  }

  async function createInvoice(e) {
    e.preventDefault()
    if (!customerName.trim()) return
    try {
      const res = await fetch(`${API}/invoices`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ customerName, items: newItems })
      })
      if (!res.ok) throw new Error()
      setCustomerName('')
      setNewItems([])
      fetchInvoices()
    } catch {
      setError('Failed to create invoice')
    }
  }

  async function payInvoice(id) {
    const method = prompt('Payment method (e.g., CASH, CARD, BANK_TRANSFER):')
    const amount = prompt('Amount:')
    if (!method || !amount) return
    
    try {
      const res = await fetch(`${API}/invoices/${id}/payments`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 
          method: method.toUpperCase(), 
          amount: amount,
          date: new Date().toISOString().split('T')[0] // Current date in YYYY-MM-DD format
        })
      })
      
      if (!res.ok) {
        const errorData = await res.json().catch(() => ({}))
        throw new Error(errorData.error || 'Failed to process payment')
      }
      
      fetchInvoices()
    } catch (err) {
      setError(err.message || 'Failed to pay invoice')
    }
  }

  async function doSearch(e) {
    e.preventDefault()
    try {
      const res = await fetch(`${API}/search?q=${encodeURIComponent(search)}`)
      const data = await res.json()
      setInvoices(data)
    } catch {
      setError('Search failed')
    }
  }

  function updateRowItem(invId, field, value) {
    setRowItems(prev => ({
      ...prev,
      [invId]: { ...(prev[invId] || { desc: '', price: '' }), [field]: value }
    }))
  }

  async function addItemToInvoice(invId) {
    const entry = rowItems[invId] || { desc: '', price: '' }
    const desc = String(entry.desc || '').trim()
    const priceNum = parseFloat(String(entry.price).replace(',', '.'))
    if (!desc) return
    if (Number.isNaN(priceNum) || !Number.isFinite(priceNum) || priceNum < 0) return
    try {
      const res = await fetch(`${API}/invoices/${invId}/items`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ description: desc, price: priceNum })
      })
      if (!res.ok) throw new Error()
      setRowItems(prev => ({ ...prev, [invId]: { desc: '', price: '' } }))
      fetchInvoices()
    } catch {
      setError('Failed to add item')
    }
  }

  async function deleteInvoice(id) {
    if (!confirm('Delete this invoice? This cannot be undone.')) return
    try {
      const res = await fetch(`${API}/invoices/${id}`, { method: 'DELETE' })
      if (!res.ok && res.status !== 204) throw new Error()
      fetchInvoices()
    } catch {
      setError('Failed to delete invoice')
    }
  }

  return (
    <div style={{ fontFamily: 'system-ui, sans-serif', margin: '2rem auto', maxWidth: 900 }}>
      <h1>Invoice App</h1>
      <form onSubmit={createInvoice} style={{ marginBottom: '1rem' }}>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center', marginBottom: 8 }}>
          <input value={customerName} onChange={e => setCustomerName(e.target.value)} placeholder="Customer name" />
        </div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center', marginBottom: 8 }}>
          <input style={{ minWidth: 260 }} value={newItemDesc} onChange={e => setNewItemDesc(e.target.value)} placeholder="Item description" />
          <input style={{ width: 120 }} value={newItemPrice} onChange={e => setNewItemPrice(e.target.value)} placeholder="Price" />
          <button onClick={addItem} type="button">Add item</button>
        </div>
        {newItems.length > 0 && (
          <div style={{ marginBottom: 8 }}>
            <strong>Items:</strong>
            <ul style={{ marginTop: 6 }}>
              {newItems.map((it) => (
                <li key={it.id}>
                  {it.description} — {it.price}
                  <button type="button" style={{ marginLeft: 8 }} onClick={() => removeItem(it.id)}>Remove</button>
                </li>
              ))}
            </ul>
          </div>
        )}
        <button type="submit">Create</button>
      </form>
      <form onSubmit={doSearch} style={{ marginBottom: '1rem' }}>
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search invoices" />
        <button type="submit">Search</button>
        <button type="button" onClick={fetchInvoices} style={{ marginLeft: 8 }}>Clear</button>
      </form>
      {loading && <p>Loading...</p>}
      {error && <p style={{ color: 'red' }}>{error}</p>}
      <table width="100%" cellPadding="6" style={{ borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            <th align="left">ID</th>
            <th align="left">Customer</th>
            <th align="left">Date</th>
            <th align="left">Total</th>
            <th align="left">Paid</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {invoices.map(inv => (
            <tr key={inv.id} style={{ borderTop: '1px solid #ddd' }}>
              <td>{inv.id}</td>
              <td>{inv.customerName}</td>
              <td>{inv.date}</td>
              <td>{inv.total}</td>
              <td>{inv.paid ? `YES (${inv.amountPaid} via ${inv.paymentMethod})` : 'NO'}</td>
              <td>
                <div style={{ display: 'flex', gap: 6, alignItems: 'center', marginBottom: 6 }}>
                  <button type="button" onClick={() => openDetails(inv)}>Details</button>
                </div>
                {!inv.paid && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                    <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                      <input
                        style={{ minWidth: 180 }}
                        placeholder="New item description"
                        value={(rowItems[inv.id]?.desc) || ''}
                        onChange={e => updateRowItem(inv.id, 'desc', e.target.value)}
                      />
                      <input
                        style={{ width: 110 }}
                        placeholder="Price"
                        value={(rowItems[inv.id]?.price) || ''}
                        onChange={e => updateRowItem(inv.id, 'price', e.target.value)}
                      />
                      <button type="button" onClick={() => addItemToInvoice(inv.id)}>Add item</button>
                    </div>
                    <div style={{ display: 'flex', gap: 6 }}>
                      <button type="button" onClick={() => payInvoice(inv.id)}>Pay</button>
                      <button type="button" onClick={() => deleteInvoice(inv.id)} style={{ color: '#b00020' }}>Delete</button>
                    </div>
                  </div>
                )}
                {inv.paid && (
                  <button type="button" onClick={() => deleteInvoice(inv.id)} style={{ color: '#b00020' }}>Delete</button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {detailsOpen && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.35)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
          <div style={{ background: '#fff', padding: 16, borderRadius: 8, width: 700, maxHeight: '80vh', overflow: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
              <h2 style={{ margin: 0 }}>Invoice {detailsOpen.id}</h2>
              <button type="button" onClick={closeDetails}>Close</button>
            </div>
            <div style={{ marginBottom: 12 }}>
              <div><strong>Customer:</strong> {detailsOpen.customerName}</div>
              <div><strong>Date:</strong> {detailsOpen.date}</div>
              <div><strong>Paid:</strong> {detailsOpen.paid ? `YES (${detailsOpen.amountPaid} via ${detailsOpen.paymentMethod})` : 'NO'}</div>
            </div>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h3 style={{ margin: '8px 0' }}>Items</h3>
                <button type="button" onClick={addDetailsItem}>Add item</button>
              </div>
              <table width="100%" cellPadding="6" style={{ borderCollapse: 'collapse' }}>
                <thead>
                  <tr>
                    <th align="left">Description</th>
                    <th align="left">Price</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {detailsItems.map((it, idx) => (
                    <tr key={idx} style={{ borderTop: '1px solid #eee' }}>
                      <td style={{ width: '70%' }}>
                        <input style={{ width: '100%' }} value={it.description} onChange={e => updateDetailsItem(idx, 'description', e.target.value)} />
                      </td>
                      <td style={{ width: '20%' }}>
                        <input style={{ width: '100%' }} value={it.price} onChange={e => updateDetailsItem(idx, 'price', e.target.value)} />
                      </td>
                      <td style={{ width: '10%' }}>
                        <button type="button" onClick={() => removeDetailsItem(idx)} style={{ color: '#b00020' }}>Delete</button>
                      </td>
                    </tr>
                  ))}
                  {detailsItems.length === 0 && (
                    <tr>
                      <td colSpan={3} style={{ color: '#666' }}>No items</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 12 }}>
              <button type="button" onClick={closeDetails}>Cancel</button>
              <button type="button" onClick={saveDetails}>Save</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}


