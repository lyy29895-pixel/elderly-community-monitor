// 跨域配置：后端接口统一带 /api 前缀，转发到后端服务
module.exports = {
    devServer: {
        port: 9876,
        client: {
            overlay: false
        },
        proxy: {
            '/api': {
                target: 'http://localhost:9090',
                changeOrigin: true
            }
        }
    }
}
