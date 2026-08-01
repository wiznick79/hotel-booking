# PostgreSQL backups and restore

The hotel platform stores data in four PostgreSQL databases: `hotel_service`,
`booking_service`, `identity_service`, and `notification_service`. A backup is a
single timestamped set containing a custom-format dump for each database, a metadata
file, and a SHA-256 checksum manifest.

The scripts deliberately use `docker exec` against the PostgreSQL container, so they
do not expose the database port or database password. Backup directories are written
under `infrastructure/.data/backups/` by default, which is ignored by Git.

## Create a backup

Run the following from the repository root while the PostgreSQL container is running:

```bash
./infrastructure/postgres/backup.sh
```

For staging, run it on the host after connecting through AWS Systems Manager Session
Manager:

```bash
aws ssm start-session --target <staging-host-instance-id>
cd /opt/hotel-booking/current
sudo BACKUP_DIRECTORY=/opt/hotel-booking/backups \
  ./infrastructure/postgres/backup.sh
```

Staging also has a systemd timer that runs daily at 02:30 UTC after the next
deployment. It uploads an encrypted archive to the private deployment bucket under
the `backups/` prefix. Terraform expires these backups after 14 days, including older
versions created by S3 versioning. Check its state with:

```bash
sudo systemctl status hotel-booking-postgres-backup.timer
sudo systemctl list-timers hotel-booking-postgres-backup.timer
```

The script prints the created directory. Treat its contents as sensitive: the dumps
contain guest and staff data. Keep them encrypted and outside version control.

## Verify a backup

The restore script verifies every dump against `SHA256SUMS` before modifying data.
For a non-destructive integrity check only:

```bash
cd infrastructure/.data/backups/<timestamp>
sha256sum --check SHA256SUMS
```

## Restore a backup

Restoring overwrites schemas and data in all four service databases. Stop the
application services first so they cannot write while the restore runs:

```bash
docker compose stop hotel-service booking-service identity-service notification-service api-gateway
./infrastructure/postgres/restore.sh --replace infrastructure/.data/backups/<timestamp>
docker compose start hotel-service booking-service identity-service notification-service api-gateway
```

For staging, use the same Compose files and runtime environment used by the deployment
script. Confirm that the backup is accessible on the host before starting a restore.

## AWS teardown checklist

Before `terraform destroy`:

1. Create a final backup on the staging host.
2. Confirm the uploaded archive is present in the private S3 bucket.
3. Copy it to a secure location outside AWS, such as an encrypted local drive.
4. Verify the checksum manifest after copying.
5. Keep the backup only for as long as needed, then securely delete it.
6. Run `terraform plan -destroy`, inspect it, and only then run `terraform destroy`.

Destroying the EC2 instance also destroys its attached application data unless it has
been copied elsewhere.
