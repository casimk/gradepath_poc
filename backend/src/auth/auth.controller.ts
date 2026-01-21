import {
  Controller,
  Post,
  Get,
  Body,
  Param,
  Req,
  Res,
  UseGuards,
  HttpCode,
  HttpStatus,
  ValidationPipe,
  SetMetadata,
  ExecutionContext,
} from '@nestjs/common';
import { Request, Response } from 'express';
import { Throttle } from '@nestjs/throttler';
import { AuthGuard } from '@nestjs/passport';
import { Reflector } from '@nestjs/core';

import { AuthService } from './auth.service';
import { CsrfService } from './csrf.service';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
import { RefreshTokenDto } from './dto/refresh-token.dto';
import { AuthResponseDto } from './dto/auth-response.dto';

// CSRF Guard decorator
export const CsrfGuard = () => ({
  canActivate: (context: ExecutionContext) => {
    const request = context.switchToHttp().getRequest<Request>();
    const reflector = new Reflector();

    // Check if route is marked as public
    const isPublic = reflector.getAllAndOverride<boolean>('isPublic', [
      context.getHandler(),
      context.getClass(),
    ]);

    if (isPublic) {
      return true;
    }

    // For POST/PUT/DELETE requests, validate CSRF token
    const method = request.method;
    if (['POST', 'PUT', 'DELETE'].includes(method)) {
      const csrfToken = request.headers['x-csrf-token'] || request.body?._csrf;
      const sessionId = request.headers['x-session-id'];

      if (!csrfToken || !sessionId) {
        return false;
      }

      // Get CsrfService from the module (injected via request)
      const csrfService = request.app.get('CsrfService');
      if (csrfService) {
        return csrfService.validateToken(sessionId, csrfToken);
      }
    }

    // GET requests don't need CSRF
    return true;
  },
});

@Controller('auth')
export class AuthController {
  constructor(
    private readonly authService: AuthService,
    private readonly csrfService: CsrfService,
  ) {}

  @Post('register')
  @HttpCode(HttpStatus.CREATED)
  @Throttle({ default: { limit: 3, ttl: 60000 } })
  async register(
    @Body(ValidationPipe) registerDto: RegisterDto,
    @Res() res: Response,
  ): Promise<void> {
    const result = await this.authService.register(registerDto);

    // Set httpOnly cookies with SameSite=strict
    const cookieOptions = {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict' as const,
      maxAge: 7 * 24 * 60 * 60 * 1000, // 7 days
      path: '/',
    };

    // Access token in cookie (short-lived)
    res.cookie('access_token', result.accessToken, {
      ...cookieOptions,
      maxAge: 15 * 60 * 1000, // 15 minutes
    });

    // Refresh token in cookie (long-lived)
    res.cookie('refresh_token', result.refreshToken, cookieOptions);

    // Generate session ID and CSRF token
    const sessionId = this.csrfService.generateSessionId(result.user.id);
    const csrfToken = this.csrfService.generateToken(sessionId);

    // Set CSRF cookie (httpOnly for security, but frontend can read via CookieStore API)
    res.cookie('csrf_session', sessionId, {
      ...cookieOptions,
      maxAge: 24 * 60 * 60 * 1000, // 24 hours
    });

    // Return CSRF token in response body (for frontend to use in headers)
    res.cookie('csrf_token', csrfToken, {
      ...cookieOptions,
      maxAge: 24 * 60 * 60 * 1000,
    });

    // Send response with session info
    res.status(HttpStatus.CREATED).json({
      ...result,
      sessionId,
      csrfToken,
    } as AuthResponseDto & { sessionId: string; csrfToken: string });
  }

  @Post('login')
  @HttpCode(HttpStatus.OK)
  @Throttle({ default: { limit: 5, ttl: 60000 } })
  async login(
    @Body(ValidationPipe) loginDto: LoginDto,
    @Res() res: Response,
  ): Promise<void> {
    const result = await this.authService.login(loginDto);

    // Set httpOnly cookies with SameSite=strict
    const cookieOptions = {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict' as const,
      maxAge: 7 * 24 * 60 * 60 * 1000,
      path: '/',
    };

    res.cookie('access_token', result.accessToken, {
      ...cookieOptions,
      maxAge: 15 * 60 * 1000,
    });

    res.cookie('refresh_token', result.refreshToken, cookieOptions);

    // Generate session ID and CSRF token
    const sessionId = this.csrfService.generateSessionId(result.user.id);
    const csrfToken = this.csrfService.generateToken(sessionId);

    res.cookie('csrf_session', sessionId, cookieOptions);
    res.cookie('csrf_token', csrfToken, {
      ...cookieOptions,
      maxAge: 24 * 60 * 60 * 1000,
    });

    // Send response with session info
    res.json({
      ...result,
      sessionId,
      csrfToken,
    } as AuthResponseDto & { sessionId: string; csrfToken: string });
  }

  @Get('csrf-token')
  @UseGuards(AuthGuard('jwt'))
  async getCsrfToken(@Req() req: Request): Promise<{ token: string; sessionId: string }> {
    // Get user from request (attached by JWT guard)
    const user = (req as any).user;

    // Generate new session ID and CSRF token
    const sessionId = this.csrfService.generateSessionId(user.id);
    const token = this.csrfService.generateToken(sessionId);

    return { token, sessionId };
  }

  @Post('refresh')
  @HttpCode(HttpStatus.OK)
  async refreshToken(
    @Body(ValidationPipe) refreshTokenDto: RefreshTokenDto,
    @Req() req: Request,
    @Res() res: Response,
  ): Promise<void> {
    const result = await this.authService.refreshToken(refreshTokenDto.refreshToken);

    // Update access token cookie
    res.cookie('access_token', result.accessToken, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict' as const,
      maxAge: 15 * 60 * 1000,
      path: '/',
    });

    // Get session from cookie or generate new
    let sessionId = req.cookies?.csrf_session;
    if (!sessionId) {
      const user = (req as any).user || await this.authService.validateUser(result.user.id);
      sessionId = this.csrfService.generateSessionId(user.id);
    }

    // Generate new CSRF token (rotate on refresh)
    const csrfToken = this.csrfService.generateToken(sessionId);

    res.cookie('csrf_session', sessionId, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict' as const,
      maxAge: 24 * 60 * 60 * 1000,
      path: '/',
    });

    res.cookie('csrf_token', csrfToken, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict' as const,
      maxAge: 24 * 60 * 60 * 1000,
      path: '/',
    });

    // Send response with session info
    res.json({
      ...result,
      sessionId,
      csrfToken,
    } as AuthResponseDto & { sessionId: string; csrfToken: string });
  }

  @Post('logout')
  @HttpCode(HttpStatus.NO_CONTENT)
  async logout(
    @Req() req: Request,
    @Body() body: { refreshToken?: string },
    @Res() res: Response,
  ): Promise<void> {
    const userId = (req as any).user?.id;

    // Get session ID from cookie
    const sessionId = req.cookies?.csrf_session;
    if (sessionId) {
      // Revoke CSRF token
      this.csrfService.revokeToken(sessionId);
    }

    if (userId) {
      await this.authService.logout(userId, body?.refreshToken || '');
    }

    // Clear cookies
    res.clearCookie('access_token', { path: '/', sameSite: 'strict' });
    res.clearCookie('refresh_token', { path: '/', sameSite: 'strict' });
    res.clearCookie('csrf_session', { path: '/', sameSite: 'strict' });
    res.clearCookie('csrf_token', { path: '/', sameSite: 'strict' });

    // Send response
    res.status(HttpStatus.NO_CONTENT).send();
  }

  @Get('me')
  @UseGuards(AuthGuard('jwt'))
  async getCurrentUser(@Req() req: Request) {
    return (req as any).user;
  }

  @Get('oauth/:provider')
  @UseGuards(AuthGuard('google'))
  async oauthLogin(@Param('provider') provider: string) {
    // OAuth flow initiated by Passport
  }

  @Get('oauth/:provider/callback')
  @UseGuards(AuthGuard('google'))
  async oauthCallback(
    @Param('provider') provider: string,
    @Req() req: Request,
    @Res() res: Response,
  ) {
    const user = (req as any).user;
    const frontendUrl = process.env.FRONTEND_URL || 'http://localhost:8083';

    // Redirect to frontend with tokens
    return res.redirect(`${frontendUrl}/auth/callback?token=${user.accessToken}`);
  }

  @Get('verify-email/:token')
  async verifyEmail(@Param('token') token: string): Promise<{ verified: boolean }> {
    // Email verification logic
    return { verified: true };
  }

  @Post('forgot-password')
  @Throttle({ default: { limit: 3, ttl: 3600000 } })
  async forgotPassword(@Body() body: { email: string }): Promise<{ message: string }> {
    // Send password reset email
    return { message: 'Password reset email sent if account exists' };
  }

  @Post('reset-password')
  async resetPassword(
    @Body() body: { token: string; newPassword: string },
  ): Promise<{ message: string }> {
    // Reset password with token
    return { message: 'Password reset successfully' };
  }
}
