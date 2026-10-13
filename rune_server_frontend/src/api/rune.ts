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

export function deleteRune(id: number) {
    return api.delete(`/runes/${id}`)
}