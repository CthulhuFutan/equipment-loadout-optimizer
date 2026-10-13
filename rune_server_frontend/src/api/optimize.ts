import api from "@/api/request.ts";

export function getInit(dragonId: number) {
    return api.get(`/optimize/init`, { params: {dragonId} })
}

export function optimize(data: any) {
    return api.post('/optimize', data)
}

export function optimizeScene(data: any) {
    return api.post('/optimize/scene', data)
}