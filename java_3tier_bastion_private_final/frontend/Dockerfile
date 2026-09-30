FROM nginx:1.27-alpine
COPY nginx.template.conf /etc/nginx/templates/default.conf.template
COPY src/ /usr/share/nginx/html/
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
