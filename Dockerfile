# Dockerfile
FROM docker.elastic.co/elasticsearch/elasticsearch:8.18.4

RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch analysis-nori
