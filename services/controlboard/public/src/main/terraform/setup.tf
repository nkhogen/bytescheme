# Variables
variable "region" {
  default = "us-west-1"
}

variable "account_id" {
}

variable "app_ids" {
}

variable "lambda_function_file" {
  default = "../../../../alexa/target/bytescheme-controlboard-alexa-0.0.1-SNAPSHOT.jar"
}
variable "read_capacity" {
  default = 1
}

variable "write_capacity" {
  default = 1
}

# Provider
provider "aws" {
  region  = "${var.region}"
  profile = "default"
}

# Alexa in east only
provider "aws" {
  alias  = "east"
  region = "us-east-1"
}

resource "aws_kms_key" "controller-kms-key" {}

resource "aws_kms_alias" "controller-kms-alias" {
  name          = "alias/authentication-key"
  target_key_id = "${aws_kms_key.controller-kms-key.key_id}"
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
    values = ["ubuntu/images/hvm-ssd/ubuntu-xenial-16.04-amd64-server-20170721"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_security_group" "controlboard-security-group" {
  name        = "controlboard_security_group"
  description = "Allow controlboard access"

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port       = 0
    to_port         = 0
    protocol        = "-1"
    cidr_blocks     = ["0.0.0.0/0"]
  }
}

resource "aws_instance" "controller" {
  ami           = "${data.aws_ami.ubuntu.id}"
  instance_type = "t2.micro"
  key_name = "controlboard-${var.region}"
  security_groups = ["${aws_security_group.controlboard-security-group.name}"]
  iam_instance_profile = "${aws_iam_instance_profile.controller-profile.name}"
  associate_public_ip_address = true
  disable_api_termination = true
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
            "${aws_dynamodb_table.controller-endpoints-table.arn}",
            "${aws_dynamodb_table.scheduler-scanners-table.arn}",
            "${aws_dynamodb_table.scheduler-events-table.arn}"
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

resource "aws_iam_role" "controller-lambda-role" {
  name = "alexa_controller_lambda_role"
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

resource "aws_iam_role_policy" "controller-lambda-policy" {
    name = "alexa_controller_lambda_policy"
    role = "${aws_iam_role.controller-lambda-role.id}"
    policy = <<EOF
{
   "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "logs:CreateLogGroup",
                "logs:CreateLogStream",
                "logs:PutLogEvents"
            ],
            "Resource": "arn:aws:logs:*:*:*"
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

resource "aws_cloudwatch_log_group" "controller-log-group" {
  provider = "aws.east"
  name = "/aws/lambda/controller"
  retention_in_days = 1
}

resource "aws_lambda_permission" "controller-alexa" {
  provider      = "aws.east"
  statement_id  = "AllowExecutionFromAlexa"
  action        = "lambda:InvokeFunction"
  function_name = "${aws_lambda_function.controller-lambda.function_name}"
  principal     = "alexa-appkit.amazon.com"
}

resource "aws_lambda_function" "controller-lambda" {
  provider         = "aws.east"
  filename         = "${path.module}/${var.lambda_function_file}"
  function_name    = "controller"
  role             = "${aws_iam_role.controller-lambda-role.arn}"
  handler          = "com.bytescheme.service.controlboard.ControllerSpeechletRequestStreamHandler"
  source_code_hash = "${base64sha256(file("${path.module}/${var.lambda_function_file}"))}"
  runtime          = "java8"
  timeout          = 60
  memory_size      = 512
  environment {
    variables = {
      APP_IDS = "${var.app_ids}"
    }
  }
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

resource "aws_dynamodb_table" "scheduler-scanners-table" {
  name           = "Scanners"
  read_capacity  = "${var.read_capacity}"
  write_capacity = "${var.write_capacity}"
  hash_key       = "ID"

  attribute {
    name = "ID"
    type = "S"
  }
}

resource "aws_dynamodb_table" "scheduler-events-table" {
  name           = "Events"
  read_capacity  = "${var.read_capacity}"
  write_capacity = "${var.write_capacity}"
  hash_key       = "ID"
  range_key      = "SCHEDULER_ID"

  attribute {
    name = "ID"
    type = "S"
  }

  attribute {
    name = "SCHEDULER_ID"
    type = "S"
  }
}
