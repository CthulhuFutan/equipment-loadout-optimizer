import api from "@/api/request.ts";

export const imageUpload = (file: File, type: string) => {
    // 数据文件包装
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/upload', formData, {
        params: { type }
    })
}
