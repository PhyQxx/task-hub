import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useNotificationStore } from '@/stores'

describe('useNotificationStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('starts with empty notifications', () => {
    const store = useNotificationStore()
    expect(store.notifications).toEqual([])
    expect(store.unreadCount).toBe(0)
  })

  it('adds a notification with generated id and time', () => {
    const store = useNotificationStore()
    store.addNotification({ type: 'task', title: 'Test', message: 'Hello' })

    expect(store.notifications).toHaveLength(1)
    expect(store.notifications[0].title).toBe('Test')
    expect(store.notifications[0].read).toBe(false)
    expect(store.notifications[0].id).toBeTruthy()
    expect(store.notifications[0].time).toBeTruthy()
  })

  it('counts unread notifications', () => {
    const store = useNotificationStore()
    store.addNotification({ type: 'task', title: 'A', message: 'a' })
    store.addNotification({ type: 'task', title: 'B', message: 'b' })
    expect(store.unreadCount).toBe(2)

    store.markAsRead(store.notifications[0].id)
    expect(store.unreadCount).toBe(1)
  })

  it('marks single notification as read', () => {
    const store = useNotificationStore()
    store.addNotification({ type: 'task', title: 'A', message: 'a' })
    const id = store.notifications[0].id

    store.markAsRead(id)
    expect(store.notifications[0].read).toBe(true)
  })

  it('marks all as read', () => {
    const store = useNotificationStore()
    store.addNotification({ type: 'task', title: 'A', message: 'a' })
    store.addNotification({ type: 'task', title: 'B', message: 'b' })

    store.markAllAsRead()
    expect(store.unreadCount).toBe(0)
    expect(store.notifications.every((n) => n.read)).toBe(true)
  })

  it('clears all notifications', () => {
    const store = useNotificationStore()
    store.addNotification({ type: 'task', title: 'A', message: 'a' })
    store.addNotification({ type: 'task', title: 'B', message: 'b' })

    store.clearAll()
    expect(store.notifications).toEqual([])
    expect(store.unreadCount).toBe(0)
  })

  it('caps at 50 notifications', () => {
    const store = useNotificationStore()
    for (let i = 0; i < 55; i++) {
      store.addNotification({ type: 'task', title: `T${i}`, message: `m${i}` })
    }
    expect(store.notifications).toHaveLength(50)
  })

  it('newest notification appears first', () => {
    const store = useNotificationStore()
    store.addNotification({ type: 'task', title: 'First', message: '1' })
    store.addNotification({ type: 'task', title: 'Second', message: '2' })

    expect(store.notifications[0].title).toBe('Second')
    expect(store.notifications[1].title).toBe('First')
  })
})
