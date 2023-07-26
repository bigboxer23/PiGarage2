#!/usr/bin/env bash
host=garagepi

scp -o StrictHostKeyChecking=no -r garage.service pi@$host:~/
ssh -t pi@$host -o StrictHostKeyChecking=no "sudo mv ~/garage.service /lib/systemd/system"
ssh -t pi@$host -o StrictHostKeyChecking=no "sudo systemctl daemon-reload"
ssh -t pi@$host -o StrictHostKeyChecking=no "sudo systemctl enable garage.service"
ssh -t pi@$host -o StrictHostKeyChecking=no "sudo systemctl start garage.service"