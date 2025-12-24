/** @type {import('tailwindcss').Config} */
module.exports = {
    content: [
        "./src/**/*.{html,ts}",
    ],
    theme: {
        extend: {
            colors: {
                // Primary palette - Teal
                primary: {
                    50: '#f0fdfa',
                    100: '#ccfbf1',
                    200: '#99f6e4',
                    300: '#5eead4',
                    400: '#2dd4bf',
                    500: '#14b8a6',
                    600: '#0d9488',
                    700: '#0f766e',
                    800: '#115e59',
                    900: '#134e4a',
                },
                // Secondary - Cyan
                secondary: {
                    400: '#22d3ee',
                    500: '#06b6d4',
                    600: '#0891b2',
                },
                // Accent - Emerald
                accent: {
                    400: '#34d399',
                    500: '#10b981',
                    600: '#059669',
                },
                // Surface colors - Slate
                surface: {
                    50: '#f8fafc',
                    100: '#f1f5f9',
                    200: '#e2e8f0',
                    300: '#cbd5e1',
                    400: '#94a3b8',
                    500: '#64748b',
                    600: '#475569',
                    700: '#334155',
                    800: '#1e293b',
                    900: '#0f172a',
                    950: '#020617',
                },
            },
            animation: {
                'flash': 'flash 0.5s ease-out',
                'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
            },
            keyframes: {
                flash: {
                    '0%': {
                        boxShadow: '0 0 0 0 rgba(20, 184, 166, 0.7)',
                        transform: 'scale(1)',
                    },
                    '50%': {
                        boxShadow: '0 0 20px 10px rgba(20, 184, 166, 0.4)',
                        transform: 'scale(1.05)',
                    },
                    '100%': {
                        boxShadow: '0 0 0 0 rgba(20, 184, 166, 0)',
                        transform: 'scale(1)',
                    },
                },
            },
        },
    },
    plugins: [],
}
