import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('starts logged out when no token', () => {
    const store = useAuthStore()
    expect(store.isLoggedIn).toBe(false)
    expect(store.token).toBe('')
  })

  it('restores token from localStorage', () => {
    localStorage.setItem('token', 'test-token')
    localStorage.setItem('memberId', 'm1')
    localStorage.setItem('nickname', 'Alice')
    localStorage.setItem('role', 'admin')

    const store = useAuthStore()
    expect(store.isLoggedIn).toBe(true)
    expect(store.token).toBe('test-token')
    expect(store.nickname).toBe('Alice')
    expect(store.role).toBe('admin')
  })

  it('setUser stores data in ref and localStorage', () => {
    const store = useAuthStore()
    store.setUser({ token: 'tok', memberId: 'm1', nickname: 'Bob', role: 'member' })

    expect(store.token).toBe('tok')
    expect(store.memberId).toBe('m1')
    expect(store.nickname).toBe('Bob')
    expect(store.role).toBe('member')
    expect(localStorage.getItem('token')).toBe('tok')
    expect(localStorage.getItem('nickname')).toBe('Bob')
  })

  it('isProjectOwner returns true for admin role', () => {
    const store = useAuthStore()
    store.setUser({ token: 't', memberId: 'm', nickname: 'A', role: 'admin' })
    expect(store.isProjectOwner).toBe(true)
  })

  it('isProjectOwner returns true for project owner role', () => {
    const store = useAuthStore()
    store.setUser({ token: 't', memberId: 'm', nickname: 'A', role: 'member' })
    store.setProjectRole('owner')
    expect(store.isProjectOwner).toBe(true)
  })

  it('canEditProject returns true for member', () => {
    const store = useAuthStore()
    store.setUser({ token: 't', memberId: 'm', nickname: 'A', role: 'member' })
    store.setProjectRole('member')
    expect(store.canEditProject).toBe(true)
  })

  it('canEditProject returns false for viewer', () => {
    const store = useAuthStore()
    store.setUser({ token: 't', memberId: 'm', nickname: 'A', role: 'member' })
    store.setProjectRole('viewer')
    expect(store.canEditProject).toBe(false)
  })

  it('setProjectRole updates projectRole', () => {
    const store = useAuthStore()
    store.setProjectRole('viewer')
    expect(store.projectRole).toBe('viewer')
  })
})
