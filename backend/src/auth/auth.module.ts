import { Module } from '@nestjs/common';
import { PassportModule } from '@nestjs/passport';
import { JwtModule } from '@nestjs/jwt';
import { ThrottlerModule } from '@nestjs/throttler';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { ClientsModule, Transport } from '@nestjs/microservices';

import { SessionService } from './session.service';
import { AuthService } from './auth.service';
import { AuthController } from './auth.controller';
import { JwtStrategy } from './strategies/jwt.strategy';
import { CsrfService } from './csrf.service';

// OAuth strategies - only include if credentials are configured
const providers: any[] = [
  AuthService,
  SessionService,
  JwtStrategy,
  CsrfService,
];

@Module({
  imports: [
    PassportModule.register({ defaultStrategy: 'jwt' }),
    JwtModule.registerAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (configService: ConfigService) => ({
        secret: configService.get('JWT_SECRET') || 'gradepath-jwt-secret-key-change-in-production',
        signOptions: {
          expiresIn: configService.get('JWT_EXPIRATION') || '15m',
        },
      }),
    }),
    ThrottlerModule.forRoot([{
      ttl: 60000,
      limit: 10,
    }]),
    ClientsModule.register([
      {
        name: 'GRADPATH_CORE_SERVICE',
        transport: Transport.TCP,
        options: {
          host: process.env.GRADEPATH_CORE_HOST || 'gradepath-core',
          port: parseInt(process.env.GRADEPATH_CORE_PORT || '8081', 10),
        },
      },
    ]),
  ],
  controllers: [AuthController],
  providers,
  exports: [AuthService, SessionService, JwtStrategy, CsrfService],
})
export class AuthModule {}
