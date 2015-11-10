#!/bin/bash

mongo << EOF
rs.slaveOk()
EOF
