# Variables
variable "region" {
  default = "us-west-1"
}

variable "account_id" {
}

variable "read_capacity" {
  default = 1
}

variable "write_capacity" {
  default = 1
}

# Provider
provider "aws" {
  region                  = "${var.region}"
  profile                 = "default"
}

resource "aws_iam_instance_profile" "controller-profile" {
  name  = "controller_instance_profile"
  role = "${aws_iam_role.controller-role.name}"
}

resource "aws_iam_role" "controller-role" {
  name = "controller_instance_role"
  path = "/"

  assume_role_policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Action": "sts:AssumeRole",
            "Principal": {
               "Service": "ec2.amazonaws.com"
            },
            "Effect": "Allow",
            "Sid": ""
        }
    ]
}
EOF
}

data "aws_ami" "ubuntu" {
  most_recent = true

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-xenial-16.04-amd64-server-20170619.1"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_instance" "controller" {
  ami           = "${data.aws_ami.ubuntu.id}"
  instance_type = "t2.micro"
  key_name = "controlboard-us-west-1"
  security_groups = ["applications"]
  iam_instance_profile = "${aws_iam_instance_profile.controller-profile.name}"
  associate_public_ip_address = true
  disable_api_termination = true
  provisioner "file" {
    source = "${path.module}/../../../target/bytescheme-controlboard-public-0.0.1-SNAPSHOT.jar"
    destination = "/controlboard/bin"
  }

  tags {
    Name = "controller"
  }
}

resource "aws_iam_role_policy" "controller-dynamodb-policy" {
    name = "controller_dynamodb_policy"
    role = "${aws_iam_role.controller-role.id}"
    policy = <<EOF
{
   "Version":"2012-10-17",
   "Statement":[
      {
         "Effect":"Allow",
         "Action":[
            "dynamodb:BatchGetItem",
            "dynamodb:BatchWriteItem",
            "dynamodb:DeleteItem",
            "dynamodb:GetItem",
            "dynamodb:UpdateItem",
            "dynamodb:PutItem",
            "dynamodb:Query",
            "dynamodb:Scan"
         ],
         "Resource":[
            "${aws_dynamodb_table.controller-user-roles-table.arn}",
            "${aws_dynamodb_table.controller-user-objects-table.arn}",
            "${aws_dynamodb_table.controller-object-roles-table.arn}",
            "${aws_dynamodb_table.controller-endpoints-table.arn}"
         ]
      },
      {
          "Effect": "Allow",
          "Action": [
             "kms:Decrypt",
             "kms:Encrypt"
          ],
          "Resource": [
             "*"
          ]
      }
   ]
}
EOF
}

resource "aws_dynamodb_table" "controller-user-roles-table" {
  name           = "UserRoles"
  read_capacity  = "${var.read_capacity}"
  write_capacity = "${var.write_capacity}"
  hash_key       = "USER"

  attribute {
    name = "USER"
    type = "S"
  }
}

resource "aws_dynamodb_table" "controller-user-objects-table" {
  name           = "UserObjects"
  read_capacity  = "${var.read_capacity}"
  write_capacity = "${var.write_capacity}"
  hash_key       = "USER"
  range_key      = "OBJECT_ID"

  attribute {
    name = "USER"
    type = "S"
  }

  attribute {
    name = "OBJECT_ID"
    type = "S"
  }
}


resource "aws_dynamodb_table" "controller-object-roles-table" {
  name           = "ObjectRoles"
  read_capacity  = "${var.read_capacity}"
  write_capacity = "${var.write_capacity}"
  hash_key       = "OBJECT_ID"
  range_key      = "METHOD"

  attribute {
    name = "OBJECT_ID"
    type = "S"
  }

  attribute {
    name = "METHOD"
    type = "S"
  }
}

resource "aws_dynamodb_table" "controller-endpoints-table" {
  name           = "Endpoints"
  read_capacity  = "${var.read_capacity}"
  write_capacity = "${var.write_capacity}"
  hash_key       = "OBJECT_ID"

  attribute {
    name = "OBJECT_ID"
    type = "S"
  }
}
