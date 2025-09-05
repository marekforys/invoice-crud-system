import React, { useEffect, useState } from 'react'

const API = '/api'

export default function App() {
  const [invoices, setInvoices] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [customerName, setCustomerName] = useState('')
  const [search, setSearch] = useState('')

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

  async function createInvoice(e) {
    e.preventDefault()
    if (!customerName.trim()) return
    try {
      const res = await fetch(`${API}/invoices`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ customerName, items: [] })
      })
      if (!res.ok) throw new Error()
      setCustomerName('')
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
      const res = await fetch(`${API}/invoices/${id}/pay`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ method, amount: parseFloat(amount) })
      })
      if (!res.ok) throw new Error()
      fetchInvoices()
    } catch {
      setError('Failed to pay invoice')
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

  return (
    <div style={{ fontFamily: 'system-ui, sans-serif', margin: '2rem auto', maxWidth: 900 }}>
      <h1>Invoice App</h1>
      <form onSubmit={createInvoice} style={{ marginBottom: '1rem' }}>
        <input value={customerName} onChange={e => setCustomerName(e.target.value)} placeholder="Customer name" />
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
                {!inv.paid && <button onClick={() => payInvoice(inv.id)}>Pay</button>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}


