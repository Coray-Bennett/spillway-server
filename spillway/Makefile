.PHONY: build run clean

build:
	mvn clean package

run: build
	docker-compose up --build

clean:
	mvn clean
	docker-compose down -v