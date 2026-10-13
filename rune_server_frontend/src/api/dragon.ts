import api from "@/api/request.ts";

export function getDragons(params?: { dragonName?: string; element?: string; category?: string, pageNum?: number, pageSize?: number }) {
    return api.get('/dragons', { params })
}

export function getDragon(id: number) {
    return api.get(`/dragons/${id}`)
}

export function createDragon(data: any) {
    return api.post('/dragons', data)
}

export function updateDragon(id: number, data: any) {
    return api.put(`/dragons/${id}`, data)
}

export function deleteDragon(id: number) {
    return api.delete(`/dragons/${id}`)
}

export function getDragonEquipment(dragonId: number) {
    return api.get(`/dragons/${dragonId}/equipment`)
}

export function saveDragonEquipment(dragonId: number, runeIds: number[]) {
    return api.post(`/dragons/${dragonId}/equipment`, runeIds)
}