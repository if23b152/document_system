# Start from official Nginx lightweight image (small and fast)
FROM nginx:alpine

# Optional: set working directory for clarity/documentation
WORKDIR /usr/share/nginx/html

# Remove default Nginx "Welcome to nginx!" files
RUN rm -rf ./*

# Copy your static frontend files into the nginx web root
# (HTML pages go into the root directly)
COPY ./html /usr/share/nginx/html

# CSS and JS go into their own folders
COPY ./css /usr/share/nginx/html/css
COPY ./js /usr/share/nginx/html/js

# Copy your custom nginx configuration file
# (this overrides the default one inside the container)
COPY ./nginx.conf /etc/nginx/nginx.conf

# Expose port 80 inside the container
# (so docker-compose can map it to your host port 8080)
EXPOSE 80

# Run nginx in the foreground (daemon off) so Docker can manage it
CMD ["nginx", "-g", "daemon off;"]
