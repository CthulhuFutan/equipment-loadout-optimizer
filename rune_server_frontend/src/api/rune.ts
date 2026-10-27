import api from "@/api/request.ts";

export function getRunes(params?: { slotNumber?: number; setType?: string; mainStatType?: string }) {
    return api.get('/runes', { params })
}

export function getRune(id: number) {
    return api.get(`/runes/${id}`)
}

export function createRune(data: any) {
    return api.post('/runes', data)
}

export function updateRune(id: number, data: any) {
    return api.put(`/runes/${id}`, data)
}

export function deleteRune(id: number) {
    return api.delete(`/runes/${id}`)
}

export function setRuneLock(runeIds: number[], locked: boolean) {
    return api.put('/runes/lock', { runeIds, locked })
}