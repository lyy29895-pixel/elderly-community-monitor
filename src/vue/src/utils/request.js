import axios from 'axios'
import router from '@/router'

const request = axios.create({
    baseURL: '',
    timeout: 60000
})

request.interceptors.request.use(config => {
    config.headers['Content-Type'] = 'application/json;charset=utf-8'
    const token = sessionStorage.getItem('token')
    if (token) {
        config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
}, error => Promise.reject(error))

request.interceptors.response.use(
    response => {
        let res = response.data
        if (response.config.responseType === 'blob') {
            return res
        }
        if (typeof res === 'string') {
            res = res ? JSON.parse(res) : res
        }
        return res
    },
    error => {
        const status = error.response && error.response.status
        if (status === 401) {
            sessionStorage.removeItem('token')
            sessionStorage.removeItem('user')
            router.push('/login')
        }
        return Promise.reject(error)
    }
)

export default request
