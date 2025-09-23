# Use official lightweight Nginx image
FROM nginx:alpine

# Set working directory (optional, just for clarity)
WORKDIR /usr/share/nginx/html

# Remove default Nginx static files
RUN rm -rf ./*

# Copy all UI files (html, css, js) into Nginx web root
COPY ./html /usr/share/nginx/html
COPY ./css /usr/share/nginx/html/css
COPY ./js /usr/share/nginx/html/js

# Copy custom Nginx config if you have one
# Make sure your nginx.conf has 'daemon off;' at the end
COPY ./nginx.conf /etc/nginx/nginx.conf

# Expose port 80 to the host
EXPOSE 80

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]

# link: http://localhost:8080/