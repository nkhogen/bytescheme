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

# Create instance profile
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
