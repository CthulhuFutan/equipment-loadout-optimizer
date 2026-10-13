import api from './request'

export function login(data: any) {
    return api.post('/auth/login', data)
}
export function register(data: any) {
    return api.post('/auth/register', data)
}